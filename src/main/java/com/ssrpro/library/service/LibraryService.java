package com.ssrpro.library.service;

import com.ssrpro.library.dao.LibraryDao;
import com.ssrpro.library.dto.entity.Library;
import com.ssrpro.library.dto.request.BookDetailReq;
import com.ssrpro.library.dto.response.LibraryRes;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 도서관 정보나루 Open API — 정보공개 도서관 조회(libSrch)
 * @see <a href="http://data4library.kr/api/libSrch">libSrch</a>
 */
@Service
@RequiredArgsConstructor
public class LibraryService {

    private static final String LIB_SRCH_URL = "http://data4library.kr/api/libSrch";
    private static final String SRCH_BOOKS_URL = "http://data4library.kr/api/srchBooks";
    private static final int SYNC_PAGE_SIZE = 100;

    private final LibraryDao libraryDao;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${api.library.key}")
    private String authKey;

    public List<LibraryRes> findByLibraryCode(BookDetailReq req) {
        if (req.getLibraryCodes() == null || req.getLibraryCodes().isEmpty()) {
            return List.of();
        }

        List<Library> rawList = libraryDao.findByLibraryCode(req.getLibraryCodes());
        if (rawList == null || rawList.isEmpty()) {
            return List.of();
        }

        return rawList.stream()
                .map(LibraryRes::of)
                .toList();
    }

    /**
     * 상세 페이지 — DB 도서관 정보 + 정보나루 srchBooks로 도서관별 대출 가능 여부
     */
    public List<LibraryRes> findLibrariesForBookDetail(BookDetailReq req, String isbn) {
        List<LibraryRes> libraries = findByLibraryCode(req);
        if (isbn == null || isbn.isBlank()) {
            return libraries;
        }
        String isbn13 = isbn.replaceAll("[^0-9]", "");
        for (LibraryRes library : libraries) {
            library.setLoanAvailable(fetchLoanAvailable(library.getLibraryCode(), isbn13));
        }
        return libraries;
    }

    /**
     * 매일 새벽 3시 — 전국 공개 도서관 목록 동기화 (libSrch)
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void scheduleLibrarySync() {
        System.out.println("[Library Scheduler] 도서관 정보 동기화를 시작합니다.");
        int inserted = syncLibrariesFromApi();
        System.out.println("[Library Scheduler] 동기화 완료. 신규 등록: " + inserted + "건");
    }

    /**
     * libSrch API 페이지 순회 후 미등록 도서관만 DB 적재
     */
    public int syncLibrariesFromApi() {
        int pageNo = 1;
        int totalInserted = 0;
        Integer numFound = null;

        while (true) {
            Map<String, Object> response = fetchLibraryPage(pageNo, SYNC_PAGE_SIZE);
            if (response == null) {
                break;
            }

            if (response.containsKey("error")) {
                System.err.println("[Library Scheduler] API 오류: " + response.get("error"));
                break;
            }

            if (numFound == null && response.containsKey("numFound")) {
                numFound = parseInt(response.get("numFound"));
            }

            List<Map<String, Object>> libInfoList = extractLibInfoList(response);
            if (libInfoList.isEmpty()) {
                break;
            }

            for (Map<String, Object> libInfo : libInfoList) {
                Library library = mapToLibrary(libInfo);
                if (library == null) {
                    continue;
                }
                if (libraryDao.existsByLibraryCode(library.getLibraryCode())) {
                    continue;
                }
                if (libraryDao.insertLibrary(library)) {
                    totalInserted++;
                }
            }

            if (numFound != null && pageNo * SYNC_PAGE_SIZE >= numFound) {
                break;
            }
            if (libInfoList.size() < SYNC_PAGE_SIZE) {
                break;
            }
            pageNo++;
        }

        return totalInserted;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchLibraryPage(int pageNo, int pageSize) {
        URI targetUrl = UriComponentsBuilder
                .fromHttpUrl(LIB_SRCH_URL)
                .queryParam("authKey", authKey)
                .queryParam("pageNo", pageNo)
                .queryParam("pageSize", pageSize)
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();

        try {
            Map<String, Object> body = restTemplate.getForObject(targetUrl, Map.class);
            if (body == null || !body.containsKey("response")) {
                return null;
            }
            return (Map<String, Object>) body.get("response");
        } catch (Exception e) {
            System.err.println("[Library Scheduler] API 호출 실패 (page=" + pageNo + "): " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractLibInfoList(Map<String, Object> response) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (!response.containsKey("libs")) {
            return result;
        }

        Object libsNode = response.get("libs");
        if (libsNode instanceof List<?> libsList) {
            for (Object wrapper : libsList) {
                addLibFromWrapper(wrapper, result);
            }
        } else if (libsNode instanceof Map<?, ?> libsMap) {
            addLibFromWrapper(libsMap, result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void addLibFromWrapper(Object wrapper, List<Map<String, Object>> result) {
        if (wrapper instanceof Map<?, ?> libWrapper && libWrapper.containsKey("lib")) {
            Object libNode = libWrapper.get("lib");
            if (libNode instanceof Map<?, ?> libMap) {
                result.add((Map<String, Object>) libMap);
            }
        }
    }

    private Library mapToLibrary(Map<String, Object> libInfo) {
        String libCodeStr = stringValue(libInfo.get("libCode"));
        String libName = stringValue(libInfo.get("libName"));
        if (libCodeStr == null || libName == null) {
            return null;
        }

        Long libCode;
        try {
            libCode = Long.parseLong(libCodeStr.trim());
        } catch (NumberFormatException e) {
            System.err.println("[Library Scheduler] libCode 파싱 실패: " + libCodeStr);
            return null;
        }

        return Library.builder()
                .libraryCode(libCode)
                .libraryName(libName)
                .libraryAddr(stringValue(libInfo.get("address")))
                .libraryLat(parseDouble(libInfo.get("latitude")))
                .libraryLon(parseDouble(libInfo.get("longitude")))
                .build();
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

    private static Double parseDouble(Object value) {
        String s = stringValue(value);
        if (s == null) {
            return null;
        }
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return null;
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

    public int countAllLibrary() {
        return libraryDao.countAllLibrary();
    }

    @SuppressWarnings("unchecked")
    private Boolean fetchLoanAvailable(Long libCode, String isbn13) {
        if (libCode == null || isbn13 == null || isbn13.isBlank()) {
            return null;
        }
        URI targetUrl = UriComponentsBuilder
                .fromHttpUrl(SRCH_BOOKS_URL)
                .queryParam("authKey", authKey)
                .queryParam("libCode", libCode)
                .queryParam("isbn13", isbn13)
                .queryParam("format", "json")
                .build()
                .encode()
                .toUri();
        try {
            Map<String, Object> body = restTemplate.getForObject(targetUrl, Map.class);
            if (body == null || !body.containsKey("response")) {
                return null;
            }
            return parseLoanFromResponse((Map<String, Object>) body.get("response"));
        } catch (Exception e) {
            System.err.println("[Library] 대출 여부 조회 실패 libCode=" + libCode + ": " + e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Boolean parseLoanFromResponse(Map<String, Object> response) {
        if (response.containsKey("error")) {
            return null;
        }
        if (!response.containsKey("docs")) {
            return false;
        }
        List<Map<String, Object>> docs = extractDocList(response.get("docs"));
        if (docs.isEmpty()) {
            return false;
        }
        Object loan = docs.get(0).get("loanavailable");
        if (loan == null) {
            loan = docs.get(0).get("loanAvailable");
        }
        if (loan == null) {
            return null;
        }
        String value = String.valueOf(loan).trim().toUpperCase();
        if ("Y".equals(value) || "1".equals(value) || "TRUE".equals(value)) {
            return true;
        }
        if ("N".equals(value) || "0".equals(value) || "FALSE".equals(value)) {
            return false;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractDocList(Object docsNode) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (docsNode instanceof List<?> docsList) {
            for (Object wrapper : docsList) {
                addDocFromWrapper(wrapper, result);
            }
        } else if (docsNode instanceof Map<?, ?> docsMap) {
            addDocFromWrapper(docsMap, result);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private void addDocFromWrapper(Object wrapper, List<Map<String, Object>> result) {
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
        }
    }
}
