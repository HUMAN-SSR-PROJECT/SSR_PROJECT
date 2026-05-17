package com.ssrpro.library.dao;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.ssrpro.library.dto.request.ReadBookReq;
import com.ssrpro.library.dto.response.ReadBookRes;
import com.ssrpro.library.dto.response.ReadSoonRes;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class ReadBookDao {
  private final JdbcTemplate jdbcTemplate;

  // 읽을 책 중복 확인
  public boolean isReadSoonDuplicate(Long memberId, Long bookId) {
    String sql = """
        SELECT COUNT(*) FROM read_soon WHERE member_id = ? AND book_id = ?
        """;
    Integer rst = jdbcTemplate.queryForObject(sql, Integer.class, memberId, bookId);

    return rst != null && rst > 0;
  }

  // 읽는 중/완독 테이블 중복 확인 (1 : 중복 없음, 2 : 읽는 중, 3 : 완독)
  public int isReadBookDuplicate(Long memberId, Long bookId) {
    String sql = """
        SELECT CASE
                WHEN read_book_state = '읽는 중' THEN 2
                WHEN read_book_state = '완독' THEN 3
               END AS state
        FROM read_book
        WHERE member_id = ? AND book_id = ?
        """;
    List<Integer> rst = jdbcTemplate.query(
      sql,
      (rs, n) -> rs.getInt("STATE"),
      memberId,
      bookId
    );

    return rst.isEmpty() ? 1 : rst.get(0);
  }

  // 읽을 책 추가
  public boolean addToReadSoon(Long memberId, Long bookId) {
    String sql = """
        INSERT INTO read_soon (member_id, book_id)
        VALUES (?, ?)
        """;
    int rst = jdbcTemplate.update(sql, memberId, bookId);
    return rst > 0;
  }

  // 내 서재
  // 내 서재 - 읽을 책 조회
  public List<ReadSoonRes> readSoonList(Long memberId) {
    String sql = """
        SELECT rs.book_id, b.book_img, b.book_genre, 
               b.book_title, b.book_writer, b.book_rating 
        FROM read_soon rs 
        JOIN book b 
        ON rs.book_id = b.book_id 
        WHERE rs.member_id = ? 
        ORDER BY rs.read_soon_created_at DESC
        """;
    return jdbcTemplate.query(sql, (rs, n) -> {
      ReadSoonRes readSoonRes = new ReadSoonRes();
      readSoonRes.setBookId(rs.getLong("BOOK_ID"));
      readSoonRes.setBookImg(rs.getString("BOOK_IMG"));
      readSoonRes.setBookGenre(rs.getString("BOOK_GENRE"));
      readSoonRes.setBookTitle(rs.getString("BOOK_TITLE"));
      readSoonRes.setBookWriter(rs.getString("BOOK_WRITER"));
      readSoonRes.setBookRating(rs.getDouble("BOOK_RATING"));
      return readSoonRes;
    }, memberId);
  }

  // 내 서재 - 읽을 책 삭제
  public boolean deleteReadSoon(Long memberId, Long bookId) {
    String sql = """
        DELETE FROM read_soon 
        WHERE member_id = ? AND book_id = ?
        """;
    int rst = jdbcTemplate.update(sql, memberId, bookId);
    return rst > 0;
  }

  // 내 서재 - 읽을 책 → 읽는 중
  public boolean addToReading(Long memberId, Long bookId) {
    String sql = """
        INSERT INTO read_book (member_id, book_id) 
        VALUES (?, ?)
        """;
    int rst = jdbcTemplate.update(sql, memberId, bookId);
    return rst > 0;
  }

  // 내 서재 - 읽는 중 조회
  public List<ReadBookRes> readingList(Long memberId) {
    String sql = """
        SELECT rb.book_id, b.book_img, b.book_genre, 
               b.book_title, b.book_writer, rb.read_book_rating 
        FROM read_book rb 
        JOIN book b 
        ON rb.book_id = b.book_id 
        WHERE rb.member_id = ? AND rb.read_book_state = '읽는 중' 
        ORDER BY rb.read_book_created_at DESC
        """;
    return jdbcTemplate.query(sql, (rs, n) -> {
      ReadBookRes readBookRes = new ReadBookRes();
      readBookRes.setBookId(rs.getLong("BOOK_ID"));
      readBookRes.setBookImg(rs.getString("BOOK_IMG"));
      readBookRes.setBookGenre(rs.getString("BOOK_GENRE"));
      readBookRes.setBookTitle(rs.getString("BOOK_TITLE"));
      readBookRes.setBookWriter(rs.getString("BOOK_WRITER"));
      readBookRes.setReadBookRating(rs.getDouble("READ_BOOK_RATING"));
      return readBookRes;
    }, memberId);
  }

  // 내 서재 - 읽는 중 / 완독 삭제
  public boolean deleteReading(Long memberId, Long bookId) {
    String sql = """
        DELETE FROM read_book 
        WHERE member_id = ? AND book_id = ?
        """;
    int rst = jdbcTemplate.update(sql, memberId, bookId);
    return rst > 0;
  }

  // 내 서재 - 읽는 중 → 완독, 완독 상세 수정
  public boolean changeToReaded(ReadBookReq readBookReq, Long memberId) {
    String sql = """
        UPDATE read_book 
        SET read_book_rating = ?, read_book_end = ?, read_book_memo = ?, read_book_state = '완독' 
        WHERE book_id = ? AND member_id = ?
        """;
    int rst = jdbcTemplate.update(sql, readBookReq.getBookRating(), readBookReq.getEndDate(), readBookReq.getMemo(), readBookReq.getBookId(), memberId);
    return rst > 0;
  }

  // 내 서재 - 완독 조회
  public List<ReadBookRes> readedList(Long memberId) {
    String sql = """
        SELECT rb.book_id, b.book_img, b.book_genre, b.book_title, 
               b.book_writer, rb.read_book_rating, 
               rb.read_book_end, rb.read_book_memo 
        FROM read_book rb 
        JOIN book b 
        ON rb.book_id = b.book_id 
        WHERE rb.member_id = ? AND rb.read_book_state = '완독' 
        ORDER BY rb.read_book_created_at DESC
        """;
    return jdbcTemplate.query(sql, (rs, n) -> {
      ReadBookRes readBookRes = new ReadBookRes();
      readBookRes.setBookId(rs.getLong("BOOK_ID"));
      readBookRes.setBookImg(rs.getString("BOOK_IMG"));
      readBookRes.setBookGenre(rs.getString("BOOK_GENRE"));
      readBookRes.setBookTitle(rs.getString("BOOK_TITLE"));
      readBookRes.setBookWriter(rs.getString("BOOK_WRITER"));
      readBookRes.setReadBookRating(rs.getDouble("READ_BOOK_RATING"));
      var endTs = rs.getTimestamp("READ_BOOK_END");
      readBookRes.setReadBookEnd(endTs != null ? endTs.toLocalDateTime() : null);
      readBookRes.setReadBookMemo(rs.getString("READ_BOOK_MEMO"));
      return readBookRes;
    }, memberId);
  }

  // 내 서재 - 완독 상세 조회
  public ReadBookRes readedInfo(Long memberId, Long bookId) {
    String sql = """
        SELECT read_book_rating, read_book_end, read_book_memo 
        FROM read_book 
        WHERE member_id = ? AND book_id = ?
        """;
    return jdbcTemplate.queryForObject(sql, (rs, n) -> {
      ReadBookRes readBookRes = new ReadBookRes();
      readBookRes.setReadBookRating(rs.getDouble("READ_BOOK_RATING"));
      var endTs = rs.getTimestamp("READ_BOOK_END");
      readBookRes.setReadBookEnd(endTs != null ? endTs.toLocalDateTime() : null);
      readBookRes.setReadBookMemo(rs.getString("READ_BOOK_MEMO"));
      return readBookRes;
    }, memberId, bookId);
  }

  // 독서 분석 리포트
  // 통계 계산용 리스트
  public List<ReadBookRes> getRawData(Long memberId) {
    String sql = """
        SELECT rb.book_id, rb.read_book_start, rb.read_book_end, rb.read_book_rating, 
               b.book_genre, b.book_title, b.book_writer 
        FROM read_book rb 
        JOIN book b 
        ON rb.book_id = b.book_id 
        WHERE rb.member_id = ? AND rb.read_book_state = '완독' 
        ORDER BY rb.read_book_end DESC
        """;
    return jdbcTemplate.query(sql, (rs, n) -> {
      ReadBookRes readBookRes = new ReadBookRes();
      readBookRes.setBookId(rs.getLong("BOOK_ID"));
      var startTs = rs.getTimestamp("READ_BOOK_START");
      var endTs = rs.getTimestamp("READ_BOOK_END");
      readBookRes.setReadBookStart(startTs != null ? startTs.toLocalDateTime() : null);
      readBookRes.setReadBookEnd(endTs != null ? endTs.toLocalDateTime() : null);
      readBookRes.setReadBookRating(rs.getDouble("READ_BOOK_RATING"));
      readBookRes.setBookGenre(rs.getString("BOOK_GENRE"));
      readBookRes.setBookTitle(rs.getString("BOOK_TITLE"));
      readBookRes.setBookWriter(rs.getString("BOOK_WRITER"));
      return readBookRes;
    }, memberId);
  }
}
