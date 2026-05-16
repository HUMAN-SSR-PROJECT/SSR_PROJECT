package com.ssrpro.library.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.storage.Bucket;
import com.ssrpro.library.dao.BookDao;
import com.ssrpro.library.dto.entity.Book;
import com.ssrpro.library.dto.request.BookSearchReq;
import com.ssrpro.library.dto.response.BookRes;
import com.ssrpro.library.dto.security.CustomUser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
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
import java.util.stream.Collectors;

/**
 * 도서: 국립중앙도서관 SEOJI SearchApi 적재 · 검색 지역 필터: 정보나루 libSrchByBook
 *
 * @see <a href="https://www.nl.go.kr/seoji/SearchApi.do">ISBN 서지정보 API</a>
 */
@Service
@RequiredArgsConstructor
public class BookService {

    private static final String SEOJI_SEARCH_URL = "https://www.nl.go.kr/seoji/SearchApi.do";
    private static final String DATA4LIB_LIB_BY_BOOK_URL = "http://data4library.kr/api/libSrchByBook";
    private static final int SYNC_PAGE_SIZE = 100;
    private static final int MAX_STORY_LENGTH = 4000;
    private static final DateTimeFormatter PUBLISH_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final BookDao bookDao;
    private final Bucket storageBucket;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${api.nl.cert-key}")
    private String nlCertKey;

    @Value("${api.library.key}")
    private String libraryApiKey;

    @Value("${firebase.storage.bucket}")
    private String firebaseBucketName;

    public List<BookRes> searchBooks(BookSearchReq req) {
        String keyword = req.getKeyword();
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }

        List<Book> books = bookDao.findByKeyword(keyword.trim());
        if (books.isEmpty()) {
            return new ArrayList<>();
        }

        List<BookRes> filteredBooks = new ArrayList<>();
        String regionCode = String.valueOf(req.getCity());

        for (Book book : books) {
            String isbn = book.getBookIsbn();
            URI targetUrl = UriComponentsBuilder
                    .fromHttpUrl(DATA4LIB_LIB_BY_BOOK_URL)
                    .queryParam("authKey", libraryApiKey)
                    .queryParam("isbn", isbn)
                    .queryParam("region", regionCode)
                    .queryParam("format", "json")
                    .build()
                    .encode()
                    .toUri();

            try {
                Map<String, Object> response = restTemplate.getForObject(targetUrl, Map.class);
                List<String> libraryCodes = new ArrayList<>();

                if (response != null && response.containsKey("response")) {
                    Map<String, Object> resBody = (Map<String, Object>) response.get("response");
                    if (resBody.containsKey("libs")) {
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
                    }
                }

                if (!libraryCodes.isEmpty()) {
                    filteredBooks.add(BookRes.of(book, libraryCodes));
                }
            } catch (Exception e) {
                System.err.println("ISBN " + isbn + " 소장 도서관 조회 오류: " + e.getMessage());
            }
        }

        return filteredBooks;
    }

    public List<BookRes> findAllBooks() {
        return bookDao.findAll().stream()
                .map(book -> BookRes.of(book, null))
                .collect(Collectors.toList());
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
        return bookDao.save(book);
    }

    /**
     * 매일 02:00 — 전일·당일 출판예정일 기준 SEOJI 목록 동기화
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void scheduleBookSync() {
        System.out.println("[Book Scheduler] 국립중앙도서관 SEOJI 도서 동기화 시작");
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        int inserted = syncBooksFromSeoji(yesterday, today);
        System.out.println("[Book Scheduler] 동기화 완료. 신규 등록: " + inserted + "건");
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

        while (true) {
            List<Map<String, Object>> docs = fetchSeojiDocs(pageNo, SYNC_PAGE_SIZE, null, start, end);
            if (docs.isEmpty()) {
                break;
            }

            for (Map<String, Object> doc : docs) {
                Book book = mapSeojiDocToBook(doc);
                if (book == null || book.getBookIsbn() == null) {
                    continue;
                }
                if (bookDao.existsByIsbn(book.getBookIsbn())) {
                    continue;
                }
                if (bookDao.save(book)) {
                    totalInserted++;
                }
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
