# 동네북 (SSR_PROJECT)

주변 도서관 장서 검색과 독서 기록을 다루는 웹 서비스입니다. 디지털 컨버전스 SSR 과정 팀 프로젝트입니다.

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 3.5, Spring JDBC, Spring Security, Java 17 |
| View | Thymeleaf, Vanilla JS, Chart.js |
| DB | Oracle 11g |
| Cache | Redis (선택), HikariCP |
| External API | 카카오맵, 도서관 정보나루, 국립중앙도서관 SEOJI, Firebase |

## 주요 기능

- 도서·도서관 통합 검색 및 상세 조회
- 회원·로그인·독서 기록(읽을 책 / 읽는 중)
- 도서관 정보·대출 가능 여부 연동
- 관리자 기능

## 사전 요구 사항

- JDK 17+
- Oracle Database (XE 등)
- (선택) Redis

## 환경 변수

`src/main/resources/application.properties` 또는 OS 환경 변수로 설정합니다.

| 변수 | 설명 |
|------|------|
| `KAKAO_MAP_JS_KEY` | 카카오 지도 JavaScript 키 |
| `KAKAO_REST_API_KEY` | 카카오 Local REST API 키 |
| `NL_API_CERT_KEY` | 국립중앙도서관 SEOJI 인증키 |
| `LIBRARY_API_KEY` | 도서관 정보나루 API 키 |
| `FIREBASE_CONFIG_PATH` | Firebase 서비스 계정 JSON 경로 |
| `FIREBASE_STORAGE_BUCKET` | Firebase Storage 버킷 |

DB 접속(`spring.datasource.*`)은 로컬 Oracle 계정에 맞게 수정하세요. 스키마·초기 데이터는 `oracle.sql`을 참고합니다.

## 실행 방법

```bash
./gradlew bootRun
```

- 앱: `http://localhost:8111`

## 프로젝트 구조 (요약)

```
SSR_PROJECT/
├── src/main/java/com/ssrpro/library/
│   ├── controller/    # Book, BookDetail, Member, ReadBook, Admin …
│   ├── service/
│   └── …
├── src/main/resources/
│   ├── templates/     # Thymeleaf
│   └── application.properties
├── oracle.sql
└── scripts/
```

## API·문서

- 도서관 정보나루 연동: 저장소 내 `도서관정보나루_API_Manual.pdf`, `OPENAPI_GUIDE_v2.5.doc` 참고

## 팀

5인 팀 프로젝트 (HUMAN-SSR-PROJECT)

## 라이선스

교육용 팀 프로젝트입니다.
