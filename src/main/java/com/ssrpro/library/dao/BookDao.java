package com.ssrpro.library.dao;


import com.ssrpro.library.dto.entity.Book;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BookDao {
    private final JdbcTemplate jdbcTemplate;

    // 공통 RowMapper: DB 결과(ResultSet)를 Book 엔티티 객체로 변환
    private final RowMapper<Book> bookRowMapper = (rs, rowNum) -> Book.builder()
            .bookId(rs.getLong("BOOK_ID")) //
            .bookImg(rs.getString("BOOK_IMG"))
            .bookTitle(rs.getString("BOOK_TITLE"))
            .bookWriter(rs.getString("BOOK_WRITER"))
            .bookCompany(rs.getString("BOOK_COMPANY"))
            .bookGenre(rs.getString("BOOK_GENRE"))
            .bookYear(rs.getDate("BOOK_YEAR") != null ? rs.getDate("BOOK_YEAR").toLocalDate() : null)
            .bookIsbn(rs.getString("BOOK_ISBN"))
            .bookPages(rs.getInt("BOOK_PAGES"))
            .bookStory(rs.getString("BOOK_STORY"))
            .bookRating(rs.getDouble("BOOK_RATING"))
            .bookCreatedAt(rs.getTimestamp("BOOK_CREATED_AT") != null ?
                    rs.getTimestamp("BOOK_CREATED_AT").toLocalDateTime() : null)
            .bookUpdatedAt(rs.getTimestamp("BOOK_UPDATED_AT") != null ?
                    rs.getTimestamp("BOOK_UPDATED_AT").toLocalDateTime() : null)
            .build(); //

    // 도서 통합 검색
    public List<Book> findByKeyword(String keyword) {
        String sql = "SELECT * FROM BOOK " +
                "WHERE BOOK_TITLE LIKE ? OR BOOK_WRITER LIKE ? OR BOOK_GENRE LIKE ? " +
                "ORDER BY BOOK_ID DESC";
        String searchTag = "%" + keyword + "%";
        return jdbcTemplate.query(sql, bookRowMapper, searchTag, searchTag, searchTag);
    }

    // 도서 목록 조회
    public List<Book> findAll() {
        String sql = "SELECT * FROM BOOK ORDER BY BOOK_ID DESC";
        return jdbcTemplate.query(sql, bookRowMapper);
    }

    // 도서 상세 조회
    public Optional<Book> findById(Long bookId) {
        String sql = "SELECT * FROM BOOK WHERE BOOK_ID = ?";
        try {
            Book book = jdbcTemplate.queryForObject(sql, bookRowMapper, bookId);
            return Optional.ofNullable(book);
        } catch (EmptyResultDataAccessException e) {
            // 조회 결과가 없으면 Optional.empty()를 반환
            return Optional.empty();
        }
    }
    // 도서 저장  INSERT INTO
    public boolean save(Book book) {
        String sql = "INSERT INTO BOOK (" +
                "BOOK_ID, BOOK_IMG, BOOK_TITLE, BOOK_WRITER, BOOK_COMPANY, " +
                "BOOK_GENRE, BOOK_YEAR, BOOK_ISBN, BOOK_PAGES, BOOK_STORY" +
                ") VALUES (BOOK_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        int result = jdbcTemplate.update(sql,
                book.getBookImg(),
                book.getBookTitle(),
                book.getBookWriter(),
                book.getBookCompany(),
                book.getBookGenre(),
                book.getBookYear(),
                book.getBookIsbn(),
                book.getBookPages(),
                book.getBookStory()
        );

        return result > 0;
    }

}

