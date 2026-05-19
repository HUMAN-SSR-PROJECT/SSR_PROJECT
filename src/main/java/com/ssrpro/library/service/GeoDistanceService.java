package com.ssrpro.library.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeoDistanceService {

    private static final String KAKAO_ADDRESS_URL = "https://dapi.kakao.com/v2/local/search/address.json";
    private static final double EARTH_RADIUS_KM = 6371.0;

    private final RestTemplate restTemplate;

    @Value("${kakao.rest.key:}")
    private String kakaoRestKey;

    public Optional<double[]> geocodeAddress(String address) {
        if (address == null || address.isBlank()) {
            return Optional.empty();
        }
        String key = resolveRestApiKey();
        if (key.isBlank()) {
            return Optional.empty();
        }

        URI uri = UriComponentsBuilder.fromHttpUrl(KAKAO_ADDRESS_URL)
                .queryParam("query", address.trim())
                .build()
                .encode()
                .toUri();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + key);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    uri, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<String, Object> body = response.getBody();
            if (body == null || !body.containsKey("documents")) {
                return Optional.empty();
            }
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> documents = (List<Map<String, Object>>) body.get("documents");
            if (documents == null || documents.isEmpty()) {
                return Optional.empty();
            }
            Map<String, Object> first = documents.get(0);
            Object lat = first.get("y");
            Object lon = first.get("x");
            if (lat == null || lon == null) {
                return Optional.empty();
            }
            return Optional.of(new double[]{
                    Double.parseDouble(String.valueOf(lat)),
                    Double.parseDouble(String.valueOf(lon))
            });
        } catch (Exception e) {
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if (msg.contains("401") || msg.contains("AccessDenied") || msg.contains("KA Header")) {
                log.warn(
                        "[거리] 카카오 REST API 인증 실패 — .env의 KAKAO_REST_API_KEY(REST API 키)를 확인하세요. "
                                + "지도용 JavaScript 키(KAKAO_MAP_JS_KEY)는 서버 주소 검색에 사용할 수 없습니다.");
            } else {
                log.warn("[거리] 주소 좌표 변환 실패: {}", msg);
            }
            return Optional.empty();
        }
    }

    public double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    public String formatDistanceKm(double km) {
        if (km < 1.0) {
            return Math.round(km * 1000) + "m";
        }
        return String.format("%.1fkm", km);
    }

    public boolean isGeocodingConfigured() {
        return !resolveRestApiKey().isBlank();
    }

    /** 서버→dapi.kakao.com 호출은 REST API 키만 사용 (JavaScript 지도 키와 별도) */
    private String resolveRestApiKey() {
        if (kakaoRestKey != null && !kakaoRestKey.isBlank() && !kakaoRestKey.contains("${")) {
            return kakaoRestKey.trim();
        }
        return "";
    }
}
