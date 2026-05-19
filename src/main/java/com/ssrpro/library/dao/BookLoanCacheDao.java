package com.ssrpro.library.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Repository
@RequiredArgsConstructor
public class BookLoanCacheDao {

    private static final AtomicBoolean TABLE_MISSING_LOGGED = new AtomicBoolean(false);

    private final JdbcTemplate jdbcTemplate;

    /** @return empty=미캐시, true/false=캐시된 대출 가능 여부 */
    public Optional<Boolean> findLoanAvailable(String isbn, long libraryCode) {
        String sql = "SELECT LOAN_AVAILABLE FROM BOOK_LOAN_CACHE "
                + "WHERE BOOK_ISBN = ? AND LIBRARY_CODE = ?";
        try {
            String flag = jdbcTemplate.queryForObject(sql, String.class, isbn, libraryCode);
            if (flag == null || flag.isBlank()) {
                return Optional.empty();
            }
            return Optional.of("Y".equalsIgnoreCase(flag));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        } catch (DataAccessException e) {
            logTableMissingOnce(e);
            return Optional.empty();
        }
    }

    public void saveLoanAvailable(String isbn, long libraryCode, boolean loanAvailable) {
        String flag = loanAvailable ? "Y" : "N";
        try {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM BOOK_LOAN_CACHE WHERE BOOK_ISBN = ? AND LIBRARY_CODE = ?",
                    Integer.class,
                    isbn,
                    libraryCode);

            if (exists != null && exists > 0) {
                jdbcTemplate.update(
                        "UPDATE BOOK_LOAN_CACHE SET LOAN_AVAILABLE = ?, CACHED_AT = SYSDATE "
                                + "WHERE BOOK_ISBN = ? AND LIBRARY_CODE = ?",
                        flag, isbn, libraryCode);
            } else {
                jdbcTemplate.update(
                        "INSERT INTO BOOK_LOAN_CACHE (CACHE_ID, BOOK_ISBN, LIBRARY_CODE, LOAN_AVAILABLE) "
                                + "VALUES (BOOK_LOAN_CACHE_SEQ.NEXTVAL, ?, ?, ?)",
                        isbn, libraryCode, flag);
            }
        } catch (DataAccessException e) {
            logTableMissingOnce(e);
        }
    }

    private static void logTableMissingOnce(DataAccessException e) {
        if (TABLE_MISSING_LOGGED.compareAndSet(false, true)) {
            log.warn(
                    "[대출캐시] BOOK_LOAN_CACHE 테이블이 없거나 접근할 수 없습니다. "
                            + "Oracle에서 scripts/book_loan_cache.sql 을 실행한 뒤 앱을 재시작하세요. ({})",
                    e.getMostSpecificCause() != null
                            ? e.getMostSpecificCause().getMessage()
                            : e.getMessage());
        }
    }
}
