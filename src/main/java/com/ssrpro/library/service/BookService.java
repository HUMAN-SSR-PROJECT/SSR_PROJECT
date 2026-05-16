package com.ssrpro.library.service;


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

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookDao bookDao;

    private final Bucket storageBucket;

    // 도서 통합 검색
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${api.book.key}")
    private String authKey;

    @Value("${firebase.storage.bucket}")
    private String firebaseBucketName;

    public List<BookRes> searchBooks(BookSearchReq req) {
        List<Book> books = bookDao.findByKeyword(req.getKeyword());
        if (books.isEmpty()) {
            return new ArrayList<>();
        }
        List<BookRes> filteredBooks = new ArrayList<>();

        String regionCode = String.valueOf(req.getCity());
        for (Book book : books) {
            String isbn = book.getBookIsbn();

            URI targetUrl = UriComponentsBuilder
                    .fromHttpUrl("http://data4library.kr/api/libSrchByBook")
                    .queryParam("authKey", authKey)
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
                    BookRes bookRes = BookRes.of(book, libraryCodes);
                    filteredBooks.add(bookRes);
                }

            } catch (Exception e) {
                System.err.println("ISBN " + isbn + " 외부 API 대조 중 오류 발생: " + e.getMessage());
            }
        }

        return filteredBooks;
    }


    // 도서 목록 조회
    public List<BookRes> findAllBooks() {
        List<Book> books = bookDao.findAll();
        return books.stream()
                .map(book -> BookRes.of(book, null))
                .collect(Collectors.toList());
    }
    // 도서 상세 조회
    public BookRes getBookDetail(Long bookId) {
        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new RuntimeException("도서를 찾을 수 없습니다."));
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof CustomUser) {
            CustomUser loginUser = (CustomUser) principal;
            Long currentMemberId = loginUser.getMemberId();
            System.out.println("[도서상세조회 로그] 현재 로그인하여 책을 조회 중인 회원 PK ID: " + currentMemberId);
        }

        return BookRes.of(book, null);
    }
    // 최근 등록 도서
    public List<BookRes> getRecentBooks() {
        List<Book> books = bookDao.findRecentBooks();
        return books.stream()
                .map(book -> BookRes.of(book, null))
                .collect(Collectors.toList());
    }

    // 도서 저장
    public boolean registerBook(String isbn) {
        if (isbn == null || isbn.trim().isEmpty()) {
            return false;
        }

        // 중복 등록 방지 (DB 확인)
        if (bookDao.findByKeyword(isbn).stream().anyMatch(b -> isbn.equals(b.getBookIsbn()))) {
            System.out.println("[등록 패스] ISBN " + isbn + " 도서는 이미 시스템에 등록되어 존재합니다.");
            return false;
        }

        // 외부 정보나루 도서 상세조회 API 요청 URL 설계
        URI detailApiUrl = UriComponentsBuilder
                .fromHttpUrl("http://data4library.kr/api/srchDtlList")
                .queryParam("authKey", authKey)
                .queryParam("isbn13", isbn)
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();

        try {
            Map<String, Object> response = restTemplate.getForObject(detailApiUrl, Map.class);
            if (response == null || !response.containsKey("response")) return false;

            Map<String, Object> resBody = (Map<String, Object>) response.get("response");
            if (!resBody.containsKey("detail")) return false;

            List<Map<String, Object>> detailList = (List<Map<String, Object>>) resBody.get("detail");
            if (detailList.isEmpty() || !detailList.get(0).containsKey("book")) return false;

            // 계층 구조 안의 실 도서 정보 Node 추출
            Map<String, Object> bookData = (Map<String, Object>) detailList.get(0).get("book");

            String title = String.valueOf(bookData.get("bookname"));
            String writer = String.valueOf(bookData.get("authors"));
            String company = String.valueOf(bookData.get("publisher"));
            String story = String.valueOf(bookData.get("description"));
            String rawImgUrl = String.valueOf(bookData.get("bookImageURL"));
            String classNo = String.valueOf(bookData.get("class_no"));
            String pageStr = String.valueOf(bookData.get("page"));
            String pubDateStr = String.valueOf(bookData.get("publication_date"));

            // 파이어베이스 이미지 처리 로직 내부 메서드 실행
            String finalFirebaseImgUrl = uploadToFirebaseStorage(rawImgUrl, isbn);

            // 날짜 타입 포맷 정제 및 예외 예방 처리
            LocalDate bookYear = LocalDate.now();
            if (pubDateStr != null && pubDateStr.length() >= 4) {
                try {
                    if (pubDateStr.length() == 4) {
                        bookYear = LocalDate.parse(pubDateStr + "-01-01");
                    } else {
                        bookYear = LocalDate.parse(pubDateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                    }
                } catch (Exception e) {
                    // 연도 포맷 안전장치
                    try {
                        int year = Integer.parseInt(pubDateStr.substring(0, 4));
                        bookYear = LocalDate.of(year, 1, 1);
                    } catch (Exception ignored) {}
                }
            }

            // 페이지 수 예외 처리 및 타입 캐스팅
            int pages = 250;
            if (pageStr != null && !pageStr.isEmpty() && !pageStr.equals("null")) {
                try {
                    String numericalPages = pageStr.replaceAll("[^0-9]", "");
                    if (!numericalPages.isEmpty()) {
                        pages = Integer.parseInt(numericalPages);
                    }
                } catch (Exception ignored) {}
            }

            String genre = "기타";

            if (classNo != null && !classNo.trim().isEmpty() && !classNo.equals("null")) {
                char prefix = classNo.trim().charAt(0);

                switch (prefix) {
                    case '0': genre = "총류"; break;
                    case '1': genre = "철학"; break;
                    case '2': genre = "종교"; break;
                    case '3': genre = "사회과학"; break;
                    case '4': genre = "자연과학"; break;
                    case '5': genre = "기술과학"; break;
                    case '6': genre = "예술"; break;
                    case '7': genre = "언어"; break;
                    case '8': genre = "소설/문학"; break;
                    case '9': genre = "역사"; break;
                    default:  genre = "기타"; break;
                }
            }

            // 줄거리 데이터 null 검증 리프레시
            if (story == null || story.trim().isEmpty() || story.equals("null")) {
                story = "제공되는 도서 요약 정보 요약본이 존재하지 않는 서적입니다.";
            }

            // 데이터 셋으로 도서 객체 빌드 및 저장
            Book newBook = Book.builder()
                    .bookIsbn(isbn)
                    .bookImg(finalFirebaseImgUrl)
                    .bookTitle(title)
                    .bookWriter(writer)
                    .bookCompany(company)
                    .bookGenre(genre)
                    .bookYear(bookYear)
                    .bookPages(pages)
                    .bookStory(story)
                    .build();

            return bookDao.save(newBook);

        } catch (Exception e) {
            System.err.println("ISBN " + isbn + " 연동 적재 중 전면 중단 오류 트래킹: " + e.getMessage());
            return false;
        }
    }

    private String uploadToFirebaseStorage(String rawImgUrl, String isbn) {
        // 이미지가 공백이거나 없는 경우 에러 방지 기본값 세팅
        if (rawImgUrl == null || rawImgUrl.isEmpty() || rawImgUrl.equals("null")) {
            return "https://firebasestorage.googleapis.com/v0/b/" + firebaseBucketName + "/o/books%2Fno-image.png?alt=media";
        }

        String blobPath = "books/" + isbn + ".png";

        try (InputStream imageStream = new URL(rawImgUrl).openStream()) {
            // 주입받은 storageBucket 빈 객체에 스토리지 업로드 명령을 곧바로 하달합니다.
            // 이미 존재하는 파일은 자동 덮어쓰기(Overwrite)가 적용됩니다.
            storageBucket.create(blobPath, imageStream, "image/png");

            // 웹 프론트엔드 및 HTML <img> 태그에서 세션 키 없이 즉시 브라우징 인식이 가능한 Firebase 미디어 포맷 주소로 치환 반환
            return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    firebaseBucketName, blobPath.replace("/", "%2F"));

        } catch (Exception e) {
            System.err.println("[Firebase 업로드 경고] 파이어베이스 전송 실패로 오픈 API 기본 소스 이미지 링크로 대체 적재합니다: " + e.getMessage());
            return rawImgUrl; // 네트워크 순단 시 데이터 보전을 위한 폴백(Fallback) 안전망 처리
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void schedulePopularBooksSync() {
        System.out.println("[Scheduler Engine] 주기적 트렌드 도서 연동 자동 적재 배치를 실행합니다.");

        // 외부 도서관 정보나루의 인기 대출 도서 API 호출 주소 매핑
        URI popularApiUrl = UriComponentsBuilder
                .fromHttpUrl("http://data4library.kr/api/loanItemSrch")
                .queryParam("authKey", authKey)
                .queryParam("format", "json")
                .queryParam("pageSize", "10") // 일일 트렌드 상위 도서 10선 자동 추적 타겟
                .build()
                .encode()
                .toUri();

        try {
            Map<String, Object> response = restTemplate.getForObject(popularApiUrl, Map.class);
            if (response != null && response.containsKey("response")) {
                Map<String, Object> resBody = (Map<String, Object>) response.get("response");

                if (resBody.containsKey("docs")) {
                    List<Map<String, Object>> docsList = (List<Map<String, Object>>) resBody.get("docs");

                    int registerCount = 0;
                    for (Map<String, Object> docWrapper : docsList) {
                        if (docWrapper.containsKey("doc")) {
                            Map<String, Object> docInfo = (Map<String, Object>) docWrapper.get("doc");
                            String isbn13 = String.valueOf(docInfo.get("isbn13"));

                            if (isbn13 != null && !isbn13.isEmpty() && !isbn13.equals("null")) {
                                // 확보한 ISBN을 위에서 꼼꼼하게 다듬어놓은 registerBook 메서드로 패스하여 수집 프로세스 가동!
                                boolean isRegistered = registerBook(isbn13);
                                if (isRegistered) {
                                    registerCount++;
                                }
                            }
                        }
                    }
                    System.out.println("[Scheduler Engine] 자동 수집 수렴 완료. 총 " + registerCount + "개의 신상 도서가 신규 입고 처리되었습니다.");
                }
            }
        } catch (Exception e) {
            System.err.println("[Scheduler Engine Critical Error] 백그라운드 배치 연동 중단 발생: " + e.getMessage());
        }
    }
    // 도서 전체 개수
    public int countAllBooks() {
        return bookDao.countAllBooks();
    }
    // 장르 개수
    public int countGenreTypes() {
        return bookDao.countGenreTypes();
    }
    // 최다 장르명
    public String findMostCommonGenre() {
        return bookDao.findMostCommonGenre();
    }
    // 도서 삭제
    public void deleteById(Long bookId) {
        boolean isDeleted = bookDao.deleteById(bookId);
        if (!isDeleted) {
            throw new RuntimeException("도서 삭제에 실패했습니다.");
        }

    }
}
