package com.ssrpro.library.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Bucket;
import com.ssrpro.library.dao.BookDao;
import com.ssrpro.library.dao.BookHoldingCacheDao;
import com.ssrpro.library.dto.entity.Book;
import com.ssrpro.library.dto.request.BookSearchReq;
import com.ssrpro.library.dto.response.BookRes;
import com.ssrpro.library.dto.response.PageResult;
import com.ssrpro.library.dto.security.CustomUser;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 도서 적재: SEOJI SearchApi 또는 정보나루 loanItemSrch(인기대출) · 검색 지역 필터: libSrchByBook
 *
 * @see <a href="https://www.nl.go.kr/seoji/SearchApi.do">ISBN 서지정보 API</a>
 * @see <a href="https://www.data4library.kr/apiUtilization">정보나루 Open API</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private static final String SEOJI_SEARCH_URL = "https://www.nl.go.kr/seoji/SearchApi.do";
    private static final String DATA4LIB_LIB_BY_BOOK_URL = "http://data4library.kr/api/libSrchByBook";
    private static final String DATA4LIB_LOAN_ITEM_SRCH_URL = "http://data4library.kr/api/loanItemSrch";
    private static final int SYNC_PAGE_SIZE = 100;
    private static final int SEARCH_PAGE_SIZE = PageResult.DEFAULT_SIZE;
    private static final int SEARCH_DB_BATCH = 30;

    @Resource(name = "bookSearchExecutor")
    private ExecutorService bookSearchExecutor;
    private static final int MAX_STORY_LENGTH = 4000;
    private static final DateTimeFormatter PUBLISH_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BookDao bookDao;
    private final BookHoldingCacheDao bookHoldingCacheDao;
    private final Bucket storageBucket;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.nl.cert-key}")
    private String nlCertKey;

    @Value("${api.library.key}")
    private String libraryApiKey;

    @Value("${firebase.storage.bucket}")
    private String firebaseBucketName;

    /** 인기대출 적재 — 최대 신규 등록 권수 (0 이하면 제한 없음) */
    @Value("${app.sync.book-daily-limit:1000}")
    private int bookDailySyncLimit;

    @Value("${app.sync.book-loan-lookback-days:90}")
    private int bookLoanLookbackDays;

    @Value("${app.sync.book-region:}")
    private String bookSyncRegion;

    @Value("${app.search.max-fresh-api-calls:40}")
    private int maxFreshApiCallsPerSearch;

    /**
     * 지역 소장 필터 포함 검색 — 요청한 페이지(10건)만 정보나루 API를 호출한다.
     * 전체 건수 선집계 없음(첫 검색 지연·빈 화면 방지).
     */
    public SearchPageResult searchBooksPaged(BookSearchReq req, int page) {
        String keyword = req.getKeyword();
        if (keyword == null || keyword.isBlank()) {
            return new SearchPageResult(PageResult.empty(page, SEARCH_PAGE_SIZE), null);
        }

        long startedMs = System.currentTimeMillis();
        String trimmed = keyword.trim();
        int safePage = Math.max(page, 1);
        int startIndex = (safePage - 1) * SEARCH_PAGE_SIZE;
        int dbKeywordTotal = bookDao.countByKeyword(trimmed);

        HoldingLookupState lookupState = new HoldingLookupState();
        FilteredPageSlice slice = collectFilteredPage(
                trimmed, req.getCity(), req.getDistrict(), startIndex, SEARCH_PAGE_SIZE, lookupState);

        log.info(
                "[도서검색] keyword=\"{}\" region={} district={} page={} | DB키워드후보={} DB스캔={} 캐시={} API신규={} 지역소장={} | 화면={}건 hasNext={} | {}ms",
                trimmed,
                req.getCity(),
                req.getDistrict(),
                safePage,
                dbKeywordTotal,
                slice.dbScanned(),
                slice.cacheHits(),
                slice.freshApiCalls(),
                slice.holdingMatches(),
                slice.content().size(),
                slice.hasNext(),
                System.currentTimeMillis() - startedMs);

        if (slice.libraryApiWarning() != null) {
            log.warn("[도서검색] {}", slice.libraryApiWarning());
        } else if (dbKeywordTotal > 0 && slice.content().isEmpty()) {
            log.warn(
                    "[도서검색] \"{}\" — DB키워드후보 {}건, 스캔 {}건, 캐시 {}건, API신규 {}건, 지역소장 {}건.",
                    trimmed, dbKeywordTotal, slice.dbScanned(), slice.cacheHits(),
                    slice.freshApiCalls(), slice.holdingMatches());
        }

        return new SearchPageResult(
                PageResult.ofPage(slice.content(), safePage, SEARCH_PAGE_SIZE, slice.hasNext()),
                slice.libraryApiWarning());
    }

    public record SearchPageResult(PageResult<BookRes> page, String libraryApiWarning) {
    }

    private static final class HoldingLookupState {
        private final AtomicBoolean quotaExceeded = new AtomicBoolean(false);
        private final AtomicInteger cacheHits = new AtomicInteger(0);
        private final AtomicInteger freshApiCalls = new AtomicInteger(0);
        private volatile String userWarning;
    }

    private record FilteredPageSlice(
            List<BookRes> content,
            boolean hasNext,
            int dbScanned,
            int cacheHits,
            int freshApiCalls,
            int holdingMatches,
            String libraryApiWarning) {
    }

    private FilteredPageSlice collectFilteredPage(
            String keyword,
            int regionCode,
            int district,
            int startIndex,
            int pageSize,
            HoldingLookupState lookupState) {
        List<BookRes> buffer = new ArrayList<>(pageSize + 1);
        int matchIndex = 0;
        int dbOffset = 0;
        int dbScanned = 0;
        int holdingMatches = 0;

        outer:
        while (buffer.size() <= pageSize) {
            if (lookupState.quotaExceeded.get()) {
                break;
            }
            List<Book> batch = bookDao.findByKeywordPaged(keyword, dbOffset, SEARCH_DB_BATCH);
            if (batch.isEmpty()) {
                break;
            }

            List<CompletableFuture<Optional<BookRes>>> futures = new ArrayList<>(batch.size());
            for (Book book : batch) {
                futures.add(CompletableFuture.supplyAsync(
                        () -> resolveHoldingBook(book, regionCode, district, lookupState),
                        bookSearchExecutor));
            }

            for (int i = 0; i < batch.size(); i++) {
                dbScanned++;
                Optional<BookRes> hit = futures.get(i).join();
                if (hit.isEmpty()) {
                    continue;
                }
                holdingMatches++;
                if (matchIndex >= startIndex && buffer.size() <= pageSize) {
                    buffer.add(hit.get());
                }
                matchIndex++;
                if (buffer.size() > pageSize) {
                    break outer;
                }
            }

            dbOffset += batch.size();
            if (batch.size() < SEARCH_DB_BATCH) {
                break;
            }
        }

        boolean hasNext = buffer.size() > pageSize;
        List<BookRes> content = hasNext
                ? new ArrayList<>(buffer.subList(0, pageSize))
                : buffer;
        return new FilteredPageSlice(
                content,
                hasNext,
                dbScanned,
                lookupState.cacheHits.get(),
                lookupState.freshApiCalls.get(),
                holdingMatches,
                lookupState.userWarning);
    }

    private Optional<BookRes> resolveHoldingBook(Book book, int regionCode, int district, HoldingLookupState state) {
        if (book.getBookIsbn() == null || book.getBookIsbn().isBlank()) {
            return Optional.empty();
        }
        List<String> libraryCodes = findLibraryCodesByIsbn(book.getBookIsbn(), regionCode, district, state);
        if (libraryCodes.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(BookRes.of(book, libraryCodes));
    }

    public List<BookRes> findAllBooks() {
        return bookDao.findAll().stream()
                .map(book -> BookRes.of(book, null))
                .collect(Collectors.toList());
    }

    /** 관리자 도서 관리 — DB 검색만 (정보나루 소장 필터 없음) */
    public PageResult<BookRes> findBooksForAdmin(String keyword, int page) {
        int safePage = Math.max(page, 1);
        int offset = (safePage - 1) * SEARCH_PAGE_SIZE;
        String trimmed = keyword == null ? "" : keyword.trim();

        long total;
        List<Book> books;
        if (trimmed.isEmpty()) {
            total = bookDao.countAllBooks();
            books = bookDao.findAllPaged(offset, SEARCH_PAGE_SIZE);
        } else {
            total = bookDao.countByKeyword(trimmed);
            books = bookDao.findByKeywordPaged(trimmed, offset, SEARCH_PAGE_SIZE);
        }

        List<BookRes> content = books.stream()
                .map(book -> BookRes.of(book, null))
                .collect(Collectors.toList());
        return PageResult.of(content, safePage, SEARCH_PAGE_SIZE, total);
    }

    public BookRes getBookDetail(Long bookId) {
        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new RuntimeException("도서를 찾을 수 없습니다."));
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUser loginUser) {
            System.out.println("[도서상세조회] 회원 ID: " + loginUser.getMemberId());
        }
        return BookRes.of(book, null);
    }

    public List<BookRes> getRecentBooks() {
        return bookDao.findRecentBooks().stream()
                .map(book -> BookRes.of(book, null))
                .collect(Collectors.toList());
    }

    /**
     * ISBN 1건 — 국립중앙도서관 SEOJI 조회 후 DB 저장
     */
    public boolean registerBook(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }
        String normalizedIsbn = isbn.trim();
        if (bookDao.existsByIsbn(normalizedIsbn)) {
            System.out.println("[도서 등록 패스] ISBN " + normalizedIsbn + " — 이미 DB에 존재");
            return false;
        }

        List<Map<String, Object>> docs = fetchSeojiDocs(1, 1, normalizedIsbn, null, null);
        if (docs.isEmpty()) {
            System.err.println("[도서 등록 실패] SEOJI 결과 없음 — ISBN " + normalizedIsbn);
            return false;
        }

        Book book = mapSeojiDocToBook(docs.get(0));
        if (book == null) {
            return false;
        }
        return saveBookIfNew(book);
    }

    /**
     * BOOK_ISBN·BOOK_TITLE UNIQUE — 중복이면 스킵하고 적재는 계속한다.
     */
    private boolean saveBookIfNew(Book book) {
        if (bookDao.existsByIsbn(book.getBookIsbn())) {
            return false;
        }
        if (bookDao.existsByTitle(book.getBookTitle())) {
            return false;
        }
        try {
            return bookDao.save(book);
        } catch (DuplicateKeyException e) {
            System.err.println("[도서 적재] UNIQUE 중복 스킵 — ISBN " + book.getBookIsbn()
                    + ", 제목: " + book.getBookTitle());
            return false;
        }
    }

    private List<String> findLibraryCodesByIsbn(
            String isbn, int regionCode, int district, HoldingLookupState state) {
        String normalizedIsbn = normalizeIsbn(isbn);
        if (normalizedIsbn == null || normalizedIsbn.isBlank()) {
            return List.of();
        }

        Optional<List<String>> cached = loadCachedLibraryCodes(normalizedIsbn, regionCode, district);
        if (cached.isPresent()) {
            state.cacheHits.incrementAndGet();
            return cached.get();
        }

        if (state.quotaExceeded.get()) {
            return List.of();
        }
        if (state.freshApiCalls.get() >= maxFreshApiCallsPerSearch) {
            state.userWarning = "이번 검색에서 신규 소장 조회(API)를 "
                    + maxFreshApiCallsPerSearch + "건까지 사용했습니다. "
                    + "캐시된 도서만 표시되며, data4library.kr IP 등록 시 한도가 늘어납니다.";
            return List.of();
        }

        state.freshApiCalls.incrementAndGet();
        List<String> libraryCodes = fetchLibraryCodesFromApi(normalizedIsbn, regionCode, district, state);
        saveCachedLibraryCodes(normalizedIsbn, regionCode, district, libraryCodes);
        return libraryCodes;
    }

    private Optional<List<String>> loadCachedLibraryCodes(String isbn, int regionCode, int district) {
        try {
            return bookHoldingCacheDao.findLibraryCodes(isbn, regionCode, district);
        } catch (Exception e) {
            log.error("[소장캐시] 조회 실패 — BOOK_HOLDING_CACHE 테이블을 생성했는지 확인하세요: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void saveCachedLibraryCodes(String isbn, int regionCode, int district, List<String> libraryCodes) {
        try {
            bookHoldingCacheDao.saveLibraryCodes(isbn, regionCode, district, libraryCodes);
        } catch (Exception e) {
            log.warn("[소장캐시] 저장 실패 ISBN {}: {}", isbn, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> fetchLibraryCodesFromApi(
            String normalizedIsbn, int regionCode, int district, HoldingLookupState state) {
        List<String> libraryCodes = new ArrayList<>();
        UriComponentsBuilder urlBuilder = UriComponentsBuilder
                .fromHttpUrl(DATA4LIB_LIB_BY_BOOK_URL)
                .queryParam("authKey", libraryApiKey)
                .queryParam("isbn", normalizedIsbn)
                .queryParam("region", String.valueOf(regionCode))
                .queryParam("format", "json");
        if (district > 0) {
            urlBuilder.queryParam("dtl_region", String.valueOf(district));
        }
        URI targetUrl = urlBuilder.build().encode().toUri();

        try {
            Map<String, Object> response = restTemplate.getForObject(targetUrl, Map.class);
            if (response == null || !response.containsKey("response")) {
                return libraryCodes;
            }
            Map<String, Object> resBody = (Map<String, Object>) response.get("response");
            if (resBody.containsKey("error")) {
                Object error = resBody.get("error");
                if (LibraryApiQuotaException.isQuotaMessage(error)) {
                    state.quotaExceeded.set(true);
                    state.userWarning = LibraryApiQuotaException.USER_MESSAGE;
                    log.error("[libSrchByBook] API 일일 한도 초과 — 이후 호출 중단: {}", error);
                } else {
                    log.warn("[libSrchByBook] ISBN {} region={} 오류: {}",
                            normalizedIsbn, regionCode, error);
                }
                return libraryCodes;
            }
            if (!resBody.containsKey("libs")) {
                return libraryCodes;
            }
            List<Map<String, Object>> libsList = (List<Map<String, Object>>) resBody.get("libs");
            for (Map<String, Object> libWrapper : libsList) {
                if (libWrapper.containsKey("lib")) {
                    Map<String, Object> libInfo = (Map<String, Object>) libWrapper.get("lib");
                    String libCode = String.valueOf(libInfo.get("libCode"));
                    if (libCode != null && !libCode.equals("null")) {
                        libraryCodes.add(libCode);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[libSrchByBook] ISBN {} region={} 호출 실패: {}",
                    normalizedIsbn, regionCode, e.getMessage());
        }
        return libraryCodes;
    }

    /**
     * 출판예정일 구간으로 SEOJI 페이지 순회 후 미등록 도서만 저장
     */
    public int syncBooksFromSeoji(LocalDate startDate, LocalDate endDate) {
        String start = startDate.format(PUBLISH_DATE_FMT);
        String end = endDate.format(PUBLISH_DATE_FMT);

        int pageNo = 1;
        int totalInserted = 0;
        Integer totalCount = null;
        int maxInsert = bookDailySyncLimit > 0 ? bookDailySyncLimit : Integer.MAX_VALUE;

        outer:
        while (true) {
            List<Map<String, Object>> docs = fetchSeojiDocs(pageNo, SYNC_PAGE_SIZE, null, start, end);
            if (docs.isEmpty()) {
                break;
            }

            for (Map<String, Object> doc : docs) {
                if (totalInserted >= maxInsert) {
                    break outer;
                }
                Book book = mapSeojiDocToBook(doc);
                if (book == null || book.getBookIsbn() == null) {
                    continue;
                }
                if (bookDao.existsByIsbn(book.getBookIsbn()) || bookDao.existsByTitle(book.getBookTitle())) {
                    continue;
                }
                if (saveBookIfNew(book)) {
                    totalInserted++;
                }
            }

            if (totalInserted >= maxInsert) {
                System.out.println("[Book Scheduler] 일일 신규 등록 상한(" + bookDailySyncLimit
                        + "건) 도달 — 나머지는 다음 스케줄까지 보류");
                break;
            }

            if (totalCount == null) {
                totalCount = fetchSeojiTotalCount(pageNo, SYNC_PAGE_SIZE, start, end);
            }
            if (totalCount != null && pageNo * SYNC_PAGE_SIZE >= totalCount) {
                break;
            }
            if (docs.size() < SYNC_PAGE_SIZE) {
                break;
            }
            pageNo++;
        }

        return totalInserted;
    }

    /**
     * 정보나루 loanItemSrch — 인기대출 도서를 DB에 적재 (SEOJI 승인 전 테스트용)
     */
    public int syncBooksFromPopularLoan() {
        if (libraryApiKey == null || libraryApiKey.isBlank()) {
            System.err.println("[인기대출 동기화] LIBRARY_API_KEY(api.library.key)가 비어 있습니다.");
            return 0;
        }

        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(Math.max(bookLoanLookbackDays, 1));
        String startDt = start.toString();
        String endDt = end.toString();

        System.out.println("[인기대출 동기화] loanItemSrch 조회 " + startDt + " ~ " + endDt
                + ", 상한 " + (bookDailySyncLimit > 0 ? bookDailySyncLimit + "권" : "없음"));

        int pageNo = 1;
        int totalInserted = 0;
        Integer numFound = null;
        int maxInsert = bookDailySyncLimit > 0 ? bookDailySyncLimit : Integer.MAX_VALUE;

        outer:
        while (true) {
            Map<String, Object> response = fetchPopularLoanPage(pageNo, SYNC_PAGE_SIZE, startDt, endDt);
            if (response == null) {
                System.err.println("[인기대출 동기화] page=" + pageNo + " 응답 없음 — API 키·네트워크를 확인하세요.");
                break;
            }
            if (response.containsKey("error")) {
                System.err.println("[인기대출 동기화] 정보나루 API 오류: " + response.get("error"));
                break;
            }
            if (numFound == null && response.containsKey("numFound")) {
                numFound = parseInt(response.get("numFound"));
            }

            List<Map<String, Object>> docList = extractData4LibraryDocs(response);
            System.out.println("[인기대출 동기화] page=" + pageNo
                    + ", 조회 " + docList.size() + "건"
                    + (numFound != null ? ", 전체 " + numFound + "건" : ""));
            if (docList.isEmpty()) {
                System.out.println("[인기대출 동기화] page=" + pageNo + " 도서 목록 비어 있음 — 종료");
                break;
            }

            int skippedExisting = 0;
            for (Map<String, Object> doc : docList) {
                if (totalInserted >= maxInsert) {
                    break outer;
                }
                Book book = mapData4LibraryLoanDocToBook(doc);
                if (book == null || book.getBookIsbn() == null) {
                    continue;
                }
                if (bookDao.existsByIsbn(book.getBookIsbn()) || bookDao.existsByTitle(book.getBookTitle())) {
                    skippedExisting++;
                    continue;
                }
                if (saveBookIfNew(book)) {
                    totalInserted++;
                }
            }

            if (skippedExisting == docList.size()) {
                System.out.println("[인기대출 동기화] 이미 적재된 도서만 조회됨 — 추가 API 호출 중단");
                break;
            }

            if (totalInserted >= maxInsert) {
                System.out.println("[인기대출 동기화] 신규 등록 상한(" + bookDailySyncLimit + "건) 도달");
                break;
            }
            if (numFound != null && pageNo * SYNC_PAGE_SIZE >= numFound) {
                break;
            }
            if (docList.size() < SYNC_PAGE_SIZE) {
                break;
            }
            pageNo++;
        }

        return totalInserted;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchPopularLoanPage(int pageNo, int pageSize, String startDt, String endDt) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(DATA4LIB_LOAN_ITEM_SRCH_URL)
                .queryParam("authKey", libraryApiKey)
                .queryParam("startDt", startDt)
                .queryParam("endDt", endDt)
                .queryParam("pageNo", pageNo)
                .queryParam("pageSize", pageSize)
                .queryParam("format", "json");

        if (bookSyncRegion != null && !bookSyncRegion.isBlank()) {
            builder.queryParam("region", bookSyncRegion.trim());
        }

        URI uri = builder.build().encode().toUri();
        try {
            Map<String, Object> body = restTemplate.getForObject(uri, Map.class);
            if (body == null) {
                System.err.println("[인기대출 동기화] HTTP 본문 없음 (page=" + pageNo + ")");
                return null;
            }
            if (!body.containsKey("response")) {
                System.err.println("[인기대출 동기화] response 필드 없음 (page=" + pageNo + "): " + body.keySet());
                return null;
            }
            return (Map<String, Object>) body.get("response");
        } catch (Exception e) {
            System.err.println("[인기대출 동기화] loanItemSrch 호출 실패 (page=" + pageNo + "): " + e.getMessage());
            return null;
        }
    }

    private Book mapData4LibraryLoanDocToBook(Map<String, Object> doc) {
        String isbn = normalizeIsbn(fieldValue(doc,
                "isbn13", "isbn", "ISBN", "isbn10", "ea_isbn"));
        String title = fieldValue(doc, "bookname", "bookName", "title", "BOOKNAME");
        if (isbn == null || title == null) {
            return null;
        }

        String author = nullToDefault(fieldValue(doc, "authors", "author", "AUTHOR"), "저자 미상");
        String publisher = nullToDefault(fieldValue(doc, "publisher", "PUBLISHER"), "출판사 미상");
        String pubYear = fieldValue(doc, "publication_year", "publicationYear", "pubYear", "year");
        String kdc = fieldValue(doc, "kdc", "KDC", "class_no");
        String loanCount = fieldValue(doc, "loan_count", "loanCount", "ranking");
        String coverUrl = fieldValue(doc, "bookImageURL", "book_image_url", "cover", "imageUrl");

        String imgUrl = uploadToFirebaseStorage(coverUrl, isbn);
        LocalDate bookYear = parsePublicationYear(pubYear);
        String genre = mapKdcToGenre(kdc);
        int pages = Math.max(parsePages(fieldValue(doc, "page", "pages")), 1);
        String story = buildPopularLoanStory(loanCount, doc);

        return Book.builder()
                .bookIsbn(isbn)
                .bookImg(imgUrl)
                .bookTitle(title)
                .bookWriter(author)
                .bookCompany(publisher)
                .bookGenre(genre)
                .bookYear(bookYear)
                .bookPages(pages)
                .bookStory(story)
                .build();
    }

    private static String buildPopularLoanStory(String loanCount, Map<String, Object> doc) {
        String description = fieldValue(doc, "description", "bookDescription", "book_introduction");
        if (description != null && !description.isBlank()) {
            return truncateStory(description);
        }
        if (loanCount != null && !loanCount.isBlank()) {
            return "정보나루 인기 대출 도서입니다. (대출 " + loanCount + "회)";
        }
        return "정보나루 인기 대출 도서입니다.";
    }

    private static String mapKdcToGenre(String kdc) {
        if (kdc == null || kdc.isEmpty()) {
            return "기타";
        }
        char major = kdc.trim().charAt(0);
        return switch (major) {
            case '1' -> "인문";
            case '3' -> "경제/경영";
            case '4', '5' -> "과학";
            case '8' -> "소설";
            case '9' -> "역사";
            default -> "기타";
        };
    }

    private static LocalDate parsePublicationYear(String pubYear) {
        if (pubYear == null || pubYear.isBlank()) {
            return LocalDate.now();
        }
        String digits = pubYear.replaceAll("[^0-9]", "");
        if (digits.length() >= 4) {
            try {
                return LocalDate.of(Integer.parseInt(digits.substring(0, 4)), 1, 1);
            } catch (Exception ignored) {
                // fall through
            }
        }
        return LocalDate.now();
    }

    private static String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return null;
        }
        String digits = isbn.replaceAll("[^0-9Xx]", "");
        return digits.isEmpty() ? null : digits;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractData4LibraryDocs(Map<String, Object> response) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!response.containsKey("docs")) {
            return result;
        }
        Object docsNode = response.get("docs");
        if (docsNode instanceof List<?> docsList) {
            for (Object wrapper : docsList) {
                addData4LibraryDoc(wrapper, result);
            }
        } else if (docsNode instanceof Map<?, ?> docsMap) {
            addData4LibraryDoc(docsMap, result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void addData4LibraryDoc(Object wrapper, List<Map<String, Object>> result) {
        if (wrapper instanceof Map<?, ?> docWrapper && docWrapper.containsKey("doc")) {
            Object docNode = docWrapper.get("doc");
            if (docNode instanceof Map<?, ?> docMap) {
                result.add((Map<String, Object>) docMap);
            } else if (docNode instanceof List<?> docList) {
                for (Object item : docList) {
                    if (item instanceof Map<?, ?> map) {
                        result.add((Map<String, Object>) map);
                    }
                }
            }
        } else if (wrapper instanceof Map<?, ?> flat) {
            result.add((Map<String, Object>) flat);
        }
    }

    private static Integer parseInt(Object value) {
        String s = stringValue(value);
        if (s == null) {
            return null;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchSeojiDocs(int pageNo, int pageSize, String isbn,
                                                     String startPublishDate, String endPublishDate) {
        Map<String, Object> body = callSeojiApi(pageNo, pageSize, isbn, startPublishDate, endPublishDate);
        if (body == null) {
            return List.of();
        }
        return extractDocs(body);
    }

    private Integer fetchSeojiTotalCount(int pageNo, int pageSize, String startPublishDate, String endPublishDate) {
        Map<String, Object> body = callSeojiApi(pageNo, pageSize, null, startPublishDate, endPublishDate);
        if (body == null) {
            return null;
        }
        String total = fieldValue(body, "TOTAL_COUNT");
        if (total == null) {
            return null;
        }
        try {
            return Integer.parseInt(total);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callSeojiApi(int pageNo, int pageSize, String isbn,
                                           String startPublishDate, String endPublishDate) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl(SEOJI_SEARCH_URL)
                .queryParam("cert_key", nlCertKey)
                .queryParam("result_style", "json")
                .queryParam("page_no", pageNo)
                .queryParam("page_size", pageSize);

        if (isbn != null && !isbn.isBlank()) {
            builder.queryParam("isbn", isbn.trim());
        }
        if (startPublishDate != null && endPublishDate != null) {
            builder.queryParam("start_publish_date", startPublishDate);
            builder.queryParam("end_publish_date", endPublishDate);
            builder.queryParam("sort", "INPUT_DATE");
            builder.queryParam("order_by", "DESC");
        }

        URI uri = builder.build().encode().toUri();

        try {
            Map<String, Object> response = restTemplate.getForObject(uri, Map.class);
            if (response == null) {
                return null;
            }
            if ("ERROR".equalsIgnoreCase(stringValue(response.get("RESULT")))) {
                System.err.println("[SEOJI API] " + response.get("ERR_CODE") + " — " + response.get("ERR_MESSAGE"));
                return null;
            }
            return response;
        } catch (Exception e) {
            System.err.println("[SEOJI API] 호출 실패 (page=" + pageNo + "): " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDocs(Map<String, Object> body) {
        List<Map<String, Object>> result = new ArrayList<>();
        Object docsNode = body.get("docs");
        if (docsNode == null) {
            docsNode = body.get("DOC");
        }
        if (docsNode instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    result.add((Map<String, Object>) map);
                }
            }
        } else if (docsNode instanceof Map<?, ?> single) {
            result.add((Map<String, Object>) single);
        }
        return result;
    }

    private Book mapSeojiDocToBook(Map<String, Object> doc) {
        String isbn = fieldValue(doc, "EA_ISBN", "isbn", "ISBN");
        String title = fieldValue(doc, "TITLE", "title");
        if (isbn == null || title == null) {
            return null;
        }

        String author = nullToDefault(fieldValue(doc, "AUTHOR", "author"), "저자 미상");
        String publisher = nullToDefault(fieldValue(doc, "PUBLISHER", "publisher"), "출판사 미상");
        String rawImgUrl = fieldValue(doc, "TITLE_URL", "title_url");
        String pageStr = fieldValue(doc, "PAGE", "page");
        String subject = fieldValue(doc, "SUBJECT", "subject");
        String publishPreDate = fieldValue(doc, "PUBLISH_PREDATE", "publish_predate");

        String imgUrl = uploadToFirebaseStorage(rawImgUrl, isbn);
        LocalDate bookYear = parsePublishDate(publishPreDate);
        int pages = parsePages(pageStr);
        String genre = nullToDefault(subject, "기타");
        String story = resolveBookStory(doc);

        return Book.builder()
                .bookIsbn(isbn)
                .bookImg(imgUrl)
                .bookTitle(title)
                .bookWriter(author)
                .bookCompany(publisher)
                .bookGenre(genre)
                .bookYear(bookYear)
                .bookPages(pages)
                .bookStory(story)
                .build();
    }

    /**
     * SEOJI SearchApi는 줄거리 본문 대신 URL을 준다.
     * BOOK_INTRODUCTION_URL(책소개) → 없으면 BOOK_SUMMARY_URL(책요약) 순으로 조회.
     */
    private String resolveBookStory(Map<String, Object> doc) {
        String introUrl = fieldValue(doc, "BOOK_INTRODUCTION_URL", "book_introduction_url");
        String summaryUrl = fieldValue(doc, "BOOK_SUMMARY_URL", "book_summary_url");

        String story = fetchStoryFromUrl(introUrl);
        if (story == null || story.isBlank()) {
            story = fetchStoryFromUrl(summaryUrl);
        }
        if (story == null || story.isBlank()) {
            return "제공되는 도서 요약 정보가 존재하지 않습니다.";
        }
        return story;
    }

    private String fetchStoryFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        try {
            String body = restTemplate.getForObject(URI.create(url), String.class);
            if (body == null || body.isBlank()) {
                return null;
            }
            body = body.trim();

            if (body.startsWith("{")) {
                Map<String, Object> json = objectMapper.readValue(
                        body, new TypeReference<Map<String, Object>>() {});
                String fromJson = extractStoryFromJson(json);
                if (fromJson != null && !fromJson.isBlank()) {
                    return truncateStory(fromJson);
                }
            }

            return truncateStory(stripHtml(body));
        } catch (Exception e) {
            System.err.println("[SEOJI] 소개/요약 URL 조회 실패 (" + url + "): " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String extractStoryFromJson(Map<String, Object> json) {
        String direct = fieldValue(json,
                "description", "content", "introduction", "summary",
                "BOOK_INTRODUCTION", "BOOK_SUMMARY", "bookIntroduction", "bookSummary");
        if (direct != null) {
            return direct;
        }

        for (String nestedKey : List.of("doc", "docs", "data", "result", "response")) {
            Object nested = json.get(nestedKey);
            if (nested instanceof Map<?, ?> map) {
                String text = extractStoryFromJson((Map<String, Object>) map);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
            if (nested instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> first) {
                String text = extractStoryFromJson((Map<String, Object>) first);
                if (text != null && !text.isBlank()) {
                    return text;
                }
            }
        }
        return null;
    }

    private static String stripHtml(String html) {
        return html
                .replaceAll("(?s)<script.*?>.*?</script>", " ")
                .replaceAll("(?s)<style.*?>.*?</style>", " ")
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private static String truncateStory(String story) {
        if (story.length() <= MAX_STORY_LENGTH) {
            return story;
        }
        return story.substring(0, MAX_STORY_LENGTH);
    }

    private String uploadToFirebaseStorage(String rawImgUrl, String isbn) {
        if (rawImgUrl == null || rawImgUrl.isEmpty()) {
            return "https://firebasestorage.googleapis.com/v0/b/" + firebaseBucketName + "/o/books%2Fno-image.png?alt=media";
        }

        String blobPath = "books/" + isbn + ".png";
        try (InputStream imageStream = new URL(rawImgUrl).openStream()) {
            storageBucket.create(blobPath, imageStream, "image/png");
            return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    firebaseBucketName, blobPath.replace("/", "%2F"));
        } catch (Exception e) {
            System.err.println("[Firebase] 이미지 업로드 실패, 원본 URL 사용: " + e.getMessage());
            return rawImgUrl;
        }
    }

    private static LocalDate parsePublishDate(String publishPreDate) {
        if (publishPreDate == null || publishPreDate.length() < 8) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(publishPreDate.substring(0, 8), PUBLISH_DATE_FMT);
        } catch (Exception e) {
            try {
                int year = Integer.parseInt(publishPreDate.substring(0, 4));
                return LocalDate.of(year, 1, 1);
            } catch (Exception ignored) {
                return LocalDate.now();
            }
        }
    }

    private static int parsePages(String pageStr) {
        if (pageStr == null || pageStr.isBlank()) {
            return 0;
        }
        try {
            String digits = pageStr.replaceAll("[^0-9]", "");
            if (digits.isEmpty()) {
                return 0;
            }
            return Integer.parseInt(digits);
        } catch (Exception e) {
            return 0;
        }
    }

    private static String fieldValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val == null) {
                val = map.get(key.toLowerCase());
            }
            String s = stringValue(val);
            if (s != null) {
                return s;
            }
        }
        return null;
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String s = String.valueOf(value).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return null;
        }
        return s;
    }

    private static String nullToDefault(String value, String defaultValue) {
        return value != null ? value : defaultValue;
    }

    public int countAllBooks() {
        return bookDao.countAllBooks();
    }

    public int countGenreTypes() {
        return bookDao.countGenreTypes();
    }

    public String findMostCommonGenre() {
        return bookDao.findMostCommonGenre();
    }

    public void deleteById(Long bookId) {
        if (!bookDao.deleteById(bookId)) {
            throw new RuntimeException("도서 삭제에 실패했습니다.");
        }
    }
}
