package com.ssrpro.library.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 정보나루 Open API 지역 코드 (region / dtl_region).
 * @see src/main/resources/data/region-catalog.json — 도서관정보나루 API 매뉴얼 기준
 */
public final class RegionCatalog {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final List<RegionOption> CITIES;
    private static final Map<Integer, List<RegionOption>> DISTRICTS_BY_CITY;

    static {
        try (InputStream in = RegionCatalog.class.getResourceAsStream("/data/region-catalog.json")) {
            if (in == null) {
                throw new IllegalStateException("region-catalog.json not found on classpath");
            }
            JsonNode root = MAPPER.readTree(in);
            CITIES = parseCities(root.get("cities"));
            DISTRICTS_BY_CITY = parseDistricts(root.get("districtsByCity"));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load region-catalog.json", e);
        }
    }

    private RegionCatalog() {
    }

    @Getter
    public static final class RegionOption {
        private final int code;
        private final String name;

        public RegionOption(int code, String name) {
            this.code = code;
            this.name = name;
        }
    }

    public static List<RegionOption> cities() {
        return CITIES;
    }

    public static List<RegionOption> districts(int cityCode) {
        return DISTRICTS_BY_CITY.getOrDefault(cityCode, List.of(new RegionOption(0, "전체")));
    }

    public static String cityName(int cityCode) {
        return findName(CITIES, cityCode).orElse("");
    }

    public static String districtName(int cityCode, int districtCode) {
        if (districtCode <= 0) {
            return "";
        }
        return findName(districts(cityCode), districtCode).orElse("");
    }

    public static String locationLabel(int cityCode, int districtCode) {
        String city = cityName(cityCode);
        String district = districtName(cityCode, districtCode);
        if (city.isEmpty()) {
            return "";
        }
        if (district.isEmpty()) {
            return city;
        }
        return city + " " + district;
    }

    /** 시·도 변경 시 구·군 탭을 갱신하기 위한 전체 구·군 목록(Thymeleaf → JS). */
    public static Map<String, List<Map<String, Object>>> districtsCatalog() {
        Map<String, List<Map<String, Object>>> payload = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<RegionOption>> entry : DISTRICTS_BY_CITY.entrySet()) {
            List<Map<String, Object>> items = new ArrayList<>();
            for (RegionOption option : entry.getValue()) {
                items.add(Map.of("code", option.getCode(), "name", option.getName()));
            }
            payload.put(String.valueOf(entry.getKey()), items);
        }
        return Collections.unmodifiableMap(payload);
    }

    public static String districtsCatalogJson() {
        try {
            return MAPPER.writeValueAsString(districtsCatalog());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to serialize district catalog", e);
        }
    }

    public static List<RegionOption> districtsForJson(int cityCode) {
        return Collections.unmodifiableList(districts(cityCode));
    }

    private static List<RegionOption> parseCities(JsonNode citiesNode) {
        List<RegionOption> cities = new ArrayList<>();
        for (JsonNode node : citiesNode) {
            cities.add(new RegionOption(node.get("code").asInt(), node.get("name").asText()));
        }
        return List.copyOf(cities);
    }

    private static Map<Integer, List<RegionOption>> parseDistricts(JsonNode districtsNode) {
        Map<Integer, List<RegionOption>> map = new LinkedHashMap<>();
        districtsNode.fields().forEachRemaining(entry -> {
            int cityCode = Integer.parseInt(entry.getKey());
            List<RegionOption> districts = new ArrayList<>();
            for (JsonNode node : entry.getValue()) {
                districts.add(new RegionOption(node.get("code").asInt(), node.get("name").asText()));
            }
            map.put(cityCode, List.copyOf(districts));
        });
        return Map.copyOf(map);
    }

    private static Optional<String> findName(List<RegionOption> options, int code) {
        return options.stream()
                .filter(option -> option.getCode() == code)
                .map(RegionOption::getName)
                .findFirst();
    }
}
