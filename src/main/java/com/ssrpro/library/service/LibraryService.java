package com.ssrpro.library.service;

import com.ssrpro.library.dao.BookLoanCacheDao;
import com.ssrpro.library.dao.LibraryDao;
import com.ssrpro.library.dto.entity.Library;
import com.ssrpro.library.dto.entity.Members;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 도서관 정보나루 Open API — 정보공개 도서관 조회(libSrch)
 * @see <a href="http://data4library.kr/api/libSrch">libSrch</a>
 */
@Service
@RequiredArgsConstructor
public class LibraryService {

    private static final String LIB_SRCH_URL = "http://data4library.kr/api/libSrch";
    /** 도서관별 소장·대출 가능 여부 (공식: bookExist, srchBooks는 키워드 검색용) */
    private static final String BOOK_EXIST_URL = "http://data4library.kr/api/bookExist";
    private static final int SYNC_PAGE_SIZE = 100;

    private final LibraryDao libraryDao;
    private final BookLoanCacheDao bookLoanCacheDao;
    private final GeoDistanceService geoDistanceService;
    private final MemberService memberService;
    private final RestTemplate restTemplate;

    @Value("${api.library.key}")
    private String authKey;

    @Value("${app.detail.max-fresh-loan-api-calls:20}")
    private int maxFreshLoanApiCallsPerDetail;

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

  public record BookDetailLibrariesResult(
      List<LibraryRes> libraries, String libraryApiWarning, String distanceHint) {}

  /**
   * 상세 페이지 — DB 도서관 + 회원 주소 기준 거리 + bookExist 대출 여부(캐시·한도 보호)
   */
  public BookDetailLibrariesResult findLibrariesForBookDetail(
      BookDetailReq req, String isbn, Long memberId) {
    List<LibraryRes> libraries = new ArrayList<>(findByLibraryCode(req));
    Optional<double[]> memberCoords = resolveMemberCoordinates(memberId);
    String distanceHint = resolveDistanceHint(memberId, memberCoords);

    for (LibraryRes library : libraries) {
      applyDistance(library, memberCoords);
    }
    sortByDistance(libraries);

    String libraryApiWarning = null;
    if (isbn != null && !isbn.isBlank()) {
      String isbn13 = isbn.replaceAll("[^0-9]", "");
      LoanLookupState loanState = new LoanLookupState();
      for (LibraryRes library : libraries) {
        library.setLoanAvailable(resolveLoanAvailable(isbn13, library.getLibraryCode(), loanState));
      }
      libraryApiWarning = loanState.userWarning;
    }

    return new BookDetailLibrariesResult(libraries, libraryApiWarning, distanceHint);
  }

  private Optional<double[]> resolveMemberCoordinates(Long memberId) {
    if (memberId == null) {
      return Optional.empty();
    }
    try {
      Members member = memberService.getMemberById(memberId);
      String addr = member.getAddr();
      if (addr == null || addr.isBlank()) {
        return Optional.empty();
      }
      return geoDistanceService.geocodeAddress(addr);
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  private String resolveDistanceHint(Long memberId, Optional<double[]> memberCoords) {
    if (memberId == null) {
      return "로그인 후 마이페이지에서 주소를 등록하면 거리를 확인할 수 있습니다.";
    }
    try {
      Members member = memberService.getMemberById(memberId);
      if (member.getAddr() == null || member.getAddr().isBlank()) {
        return "마이페이지에서 주소를 등록해 주세요.";
      }
    } catch (Exception e) {
      return null;
    }
    if (!geoDistanceService.isGeocodingConfigured()) {
      return "거리 표시에는 카카오 REST API 키(.env의 KAKAO_REST_API_KEY)가 필요합니다. "
          + "지도용 JavaScript 키(KAKAO_MAP_JS_KEY)와는 별도 키입니다.";
    }
    if (memberCoords.isEmpty()) {
      return "등록한 주소를 좌표로 변환하지 못했습니다. 주소를 다시 확인해 주세요.";
    }
    return null;
  }

  private void applyDistance(LibraryRes library, Optional<double[]> memberCoords) {
    if (memberCoords.isEmpty()) {
      return;
    }
    Double lat = library.getLibraryLat();
    Double lon = library.getLibraryLon();
    if (lat == null || lon == null) {
      return;
    }
    double[] origin = memberCoords.get();
    double km =
        geoDistanceService.distanceKm(origin[0], origin[1], lat, lon);
    library.setDistanceKm(km);
    library.setDistanceLabel(geoDistanceService.formatDistanceKm(km));
  }

  private void sortByDistance(List<LibraryRes> libraries) {
    boolean anyDistance =
        libraries.stream().anyMatch(lib -> lib.getDistanceKm() != null);
    if (!anyDistance) {
      return;
    }
    libraries.sort(
        Comparator.comparing(
            LibraryRes::getDistanceKm, Comparator.nullsLast(Comparator.naturalOrder())));
  }

  private static final class LoanLookupState {
    private final AtomicBoolean quotaExceeded = new AtomicBoolean(false);
    private final AtomicInteger freshApiCalls = new AtomicInteger(0);
    private String userWarning;
  }

  private Boolean resolveLoanAvailable(String isbn13, Long libCode, LoanLookupState state) {
    if (libCode == null || isbn13 == null || isbn13.isBlank()) {
      return null;
    }
    Optional<Boolean> cached = bookLoanCacheDao.findLoanAvailable(isbn13, libCode);
    if (cached.isPresent()) {
      return cached.get();
    }
    if (state.quotaExceeded.get()) {
      return null;
    }
    if (state.freshApiCalls.get() >= maxFreshLoanApiCallsPerDetail) {
      if (state.userWarning == null) {
        state.userWarning =
            "대출 여부 API 호출이 상한("
                + maxFreshLoanApiCallsPerDetail
                + "건)에 도달했습니다. "
                + LibraryApiQuotaException.USER_MESSAGE;
      }
      return null;
    }

    state.freshApiCalls.incrementAndGet();
    Boolean loan = fetchLoanAvailable(libCode, isbn13, state);
    if (loan != null) {
      try {
        bookLoanCacheDao.saveLoanAvailable(isbn13, libCode, loan);
      } catch (Exception e) {
        System.err.println("[Library] 대출 캐시 저장 실패: " + e.getMessage());
      }
    }
    return loan;
  }

    /**
     * 매일 12:00 — 전국 공개 도서관 목록 동기화 (libSrch)
     */
    @Scheduled(cron = "0 0 12 * * ?")
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
                if (libraryDao.existsByLibraryAddr(library.getLibraryAddr())) {
                    System.err.println("[Library Sync] 주소 중복 — 스킵 libCode="
                            + library.getLibraryCode() + " " + library.getLibraryName());
                    continue;
                }
                try {
                    if (libraryDao.insertLibrary(library)) {
                        totalInserted++;
                    }
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("ORA-00001")) {
                        System.err.println("[Library Sync] 중복 데이터 — 스킵 libCode="
                                + library.getLibraryCode());
                    } else {
                        System.err.println("[Library Sync] 저장 실패 libCode="
                                + library.getLibraryCode() + " — " + msg);
                    }
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

        String addr = firstString(libInfo, "address", "libAddress", "roadAddress");
        if (addr == null) {
            System.err.println("[Library Sync] 주소 없음 — 스킵 libCode=" + libCode + " " + libName);
            return null;
        }

        Double lat = parseDouble(libInfo.get("latitude"));
        Double lon = parseDouble(libInfo.get("longitude"));
        if (lat == null || lon == null) {
            System.err.println("[Library Sync] 좌표 없음 — 스킵 libCode=" + libCode + " " + libName);
            return null;
        }

        return Library.builder()
                .libraryCode(libCode)
                .libraryName(libName)
                .libraryAddr(addr)
                .libraryLat(lat)
                .libraryLon(lon)
                .build();
    }

    private static String firstString(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            String value = stringValue(map.get(key));
            if (value != null) {
                return value;
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
    private Boolean fetchLoanAvailable(Long libCode, String isbn13, LoanLookupState state) {
        if (libCode == null || isbn13 == null || isbn13.isBlank()) {
            return null;
        }
        URI targetUrl = UriComponentsBuilder
                .fromHttpUrl(BOOK_EXIST_URL)
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
            return parseBookExistResponse((Map<String, Object>) body.get("response"), state);
        } catch (Exception e) {
            System.err.println("[Library] 대출 여부 조회 실패 libCode=" + libCode + ": " + e.getMessage());
            return null;
        }
    }

  @SuppressWarnings("unchecked")
  private Boolean parseBookExistResponse(Map<String, Object> response, LoanLookupState state) {
    if (response.containsKey("error")) {
      Object error = response.get("error");
      if (state != null && LibraryApiQuotaException.isQuotaMessage(error)) {
        state.quotaExceeded.set(true);
        state.userWarning = LibraryApiQuotaException.USER_MESSAGE;
        System.err.println("[Library] bookExist API 일일 한도 초과: " + error);
      } else {
        System.err.println("[Library] bookExist API 오류: " + error);
      }
      return null;
    }

    Boolean loan = parseLoanFlag(response);
    if (loan != null) {
      return loan;
    }

    Object resultNode = response.get("result");
    if (resultNode instanceof Map<?, ?> resultMap) {
      loan = parseLoanFromBookExistResult((Map<String, Object>) resultMap);
      if (loan != null) {
        return loan;
      }
    }

    if (response.containsKey("docs")) {
      List<Map<String, Object>> docs = extractDocList(response.get("docs"));
      if (!docs.isEmpty()) {
        loan = parseLoanFlag(docs.get(0));
        if (loan != null) {
          return loan;
        }
      }
    }

    Object hasBook = firstField(response, "hasBook", "hasbook");
    if (hasBook != null && isNegativeFlag(hasBook)) {
      return false;
    }
    return null;
  }

  private static Boolean parseLoanFromBookExistResult(Map<String, Object> result) {
    Boolean loan = parseLoanFlag(result);
    if (loan != null) {
      return loan;
    }
    Object hasBook = firstField(result, "hasBook", "hasbook");
    if (hasBook != null && isNegativeFlag(hasBook)) {
      return false;
    }
    return null;
  }

  private static Boolean parseLoanFlag(Map<String, Object> map) {
    Object loan = firstField(map, "loanAvailable", "loanavailable");
    if (loan == null) {
      return null;
    }
    if (isPositiveFlag(loan)) {
      return true;
    }
    if (isNegativeFlag(loan)) {
      return false;
    }
    return null;
  }

  private static Object firstField(Map<String, Object> map, String... keys) {
    for (String key : keys) {
      if (map.containsKey(key)) {
        return map.get(key);
      }
    }
    return null;
  }

  private static boolean isPositiveFlag(Object value) {
    String s = String.valueOf(value).trim().toUpperCase();
    return "Y".equals(s) || "1".equals(s) || "TRUE".equals(s);
  }

  private static boolean isNegativeFlag(Object value) {
    String s = String.valueOf(value).trim().toUpperCase();
    return "N".equals(s) || "0".equals(s) || "FALSE".equals(s);
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
        if (!(wrapper instanceof Map<?, ?> docWrapper)) {
            return;
        }
        if (docWrapper.containsKey("doc")) {
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
            return;
        }
        result.add((Map<String, Object>) docWrapper);
    }
}
