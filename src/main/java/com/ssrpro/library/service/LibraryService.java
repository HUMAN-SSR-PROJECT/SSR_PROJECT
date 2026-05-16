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
}
