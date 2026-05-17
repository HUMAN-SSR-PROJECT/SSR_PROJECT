package com.ssrpro.library.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookHoldingCacheDao {

    private final JdbcTemplate jdbcTemplate;

    /**
     * @return empty — 캐시 없음, present(빈 리스트 포함) — 캐시 hit
     */
    public Optional<List<String>> findLibraryCodes(String isbn, int regionCode, int district) {
        String sql = "SELECT LIBRARY_CODES FROM BOOK_HOLDING_CACHE "
                + "WHERE BOOK_ISBN = ? AND REGION_CODE = ? AND DTL_REGION = ?";
        try {
            String raw = jdbcTemplate.queryForObject(sql, String.class, isbn, regionCode, district);
            if (raw == null || raw.isBlank()) {
                return Optional.of(Collections.emptyList());
            }
            return Optional.of(Arrays.asList(raw.split(",")));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public void saveLibraryCodes(String isbn, int regionCode, int district, List<String> libraryCodes) {
        String joined = libraryCodes == null || libraryCodes.isEmpty()
                ? ""
                : String.join(",", libraryCodes);

        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM BOOK_HOLDING_CACHE "
                        + "WHERE BOOK_ISBN = ? AND REGION_CODE = ? AND DTL_REGION = ?",
                Integer.class,
                isbn, regionCode, district);

        if (exists != null && exists > 0) {
            jdbcTemplate.update(
                    "UPDATE BOOK_HOLDING_CACHE SET LIBRARY_CODES = ?, CACHED_AT = SYSDATE "
                            + "WHERE BOOK_ISBN = ? AND REGION_CODE = ? AND DTL_REGION = ?",
                    joined, isbn, regionCode, district);
            return;
        }

        jdbcTemplate.update(
                "INSERT INTO BOOK_HOLDING_CACHE "
                        + "(CACHE_ID, BOOK_ISBN, REGION_CODE, DTL_REGION, LIBRARY_CODES) "
                        + "VALUES (BOOK_HOLDING_CACHE_SEQ.NEXTVAL, ?, ?, ?, ?)",
                isbn, regionCode, district, joined);
    }
}
