package com.ssrpro.library.dao;

import com.ssrpro.library.dto.entity.Review;
import com.ssrpro.library.dto.entity.ReviewLikes;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReviewDao {

  private final JdbcTemplate jdbcTemplate;

  // ──────────────────────────────────────────
  // Review RowMapper (MEMBERS JOIN 포함)
  // ──────────────────────────────────────────
  private final RowMapper<Review> reviewRowMapper = (rs, rowNum) -> Review.builder()
      .reviewId(rs.getLong("REVIEW_ID"))
      .bookId(rs.getLong("BOOK_ID"))
      .memberId(rs.getLong("MEMBER_ID"))
      .memberNickname(rs.getString("MEMBER_NICKNAME"))
      .memberImgUrl(rs.getString("MEMBER_IMGURL"))
      .reviewComment(rs.getString("REVIEW_COMMENT") != null
          ? rs.getString("REVIEW_COMMENT").trim()
          : null)
      .reviewRating(rs.getDouble("REVIEW_RATING"))
      .reviewCreatedAt(rs.getTimestamp("REVIEW_CREATED_AT") != null
          ? rs.getTimestamp("REVIEW_CREATED_AT").toLocalDateTime()
          : null)
      .reviewUpdatedAt(rs.getTimestamp("REVIEW_UPDATED_AT") != null
          ? rs.getTimestamp("REVIEW_UPDATED_AT").toLocalDateTime()
          : null)
      .build();

  // ──────────────────────────────────────────
  // Review CRUD
  // ──────────────────────────────────────────

  // 리뷰 작성 (REVIEW_SEQ로 REVIEW_ID 자동 생성)
  public int insert(Review review) {
    String sql = "INSERT INTO REVIEW " +
        "(REVIEW_ID, BOOK_ID, MEMBER_ID, REVIEW_COMMENT, REVIEW_RATING) " +
        "VALUES (REVIEW_SEQ.nextval, ?, ?, ?, ?)";
    return jdbcTemplate.update(sql,
        review.getBookId(),
        review.getMemberId(),
        review.getReviewComment(),
        review.getReviewRating());
  }

  // 리뷰 수정 (본인 확인 - MEMBER_ID 조건 포함)
  public int update(Review review) {
    String sql = "UPDATE REVIEW " +
        "SET REVIEW_COMMENT = ?, REVIEW_RATING = ?, " +
        "REVIEW_UPDATED_AT = SYSDATE " +
        "WHERE REVIEW_ID = ? AND MEMBER_ID = ?";
    return jdbcTemplate.update(sql,
        review.getReviewComment(),
        review.getReviewRating(),
        review.getReviewId(),
        review.getMemberId());
  }

  // 리뷰 삭제 (본인 확인 - MEMBER_ID 조건 포함)
  public int delete(Long reviewId, Long memberId) {
    String sql = "DELETE FROM REVIEW WHERE REVIEW_ID = ? AND MEMBER_ID = ?";
    return jdbcTemplate.update(sql, reviewId, memberId);
  }

  // 관리자 리뷰 삭제 (조건 없음 - Controller에서 관리자 여부 확인)
  public int adminDelete(Long reviewId) {
    String sql = "DELETE FROM REVIEW WHERE REVIEW_ID = ?";
    return jdbcTemplate.update(sql, reviewId);
  }

  // 도서별 리뷰 목록 조회 (MEMBERS JOIN - 닉네임, 이미지 포함)
  public List<Review> findByBookId(Long bookId) {
    String sql = "SELECT R.REVIEW_ID, R.BOOK_ID, R.MEMBER_ID, " +
        "R.REVIEW_COMMENT, R.REVIEW_RATING, " +
        "R.REVIEW_CREATED_AT, R.REVIEW_UPDATED_AT, " +
        "M.MEMBER_NICKNAME, M.MEMBER_IMGURL " +
        "FROM REVIEW R " +
        "JOIN MEMBERS M ON R.MEMBER_ID = M.MEMBER_ID " +
        "WHERE R.BOOK_ID = ? " +
        "ORDER BY R.REVIEW_CREATED_AT DESC";
    return jdbcTemplate.query(sql, reviewRowMapper, bookId);
  }

  // 리뷰 단건 조회 (MEMBERS JOIN - 닉네임, 이미지 포함)
  public Review findById(Long reviewId) {
    String sql = "SELECT R.REVIEW_ID, R.BOOK_ID, R.MEMBER_ID, " +
        "R.REVIEW_COMMENT, R.REVIEW_RATING, " +
        "R.REVIEW_CREATED_AT, R.REVIEW_UPDATED_AT, " +
        "M.MEMBER_NICKNAME, M.MEMBER_IMGURL " +
        "FROM REVIEW R " +
        "JOIN MEMBERS M ON R.MEMBER_ID = M.MEMBER_ID " +
        "WHERE R.REVIEW_ID = ?";
    return jdbcTemplate.queryForObject(sql, reviewRowMapper, reviewId);
  }

  // 회원별 리뷰 목록 조회 (MEMBERS JOIN - 닉네임, 이미지 포함)
  public List<Review> findByMemberId(Long memberId) {
    String sql = "SELECT R.REVIEW_ID, R.BOOK_ID, R.MEMBER_ID, " +
        "R.REVIEW_COMMENT, R.REVIEW_RATING, " +
        "R.REVIEW_CREATED_AT, R.REVIEW_UPDATED_AT, " +
        "M.MEMBER_NICKNAME, M.MEMBER_IMGURL " +
        "FROM REVIEW R " +
        "JOIN MEMBERS M ON R.MEMBER_ID = M.MEMBER_ID " +
        "WHERE R.MEMBER_ID = ? " +
        "ORDER BY R.REVIEW_CREATED_AT DESC";
    return jdbcTemplate.query(sql, reviewRowMapper, memberId);
  }

  // 전체 리뷰 수 조회
  public int countByBookId(Long bookId) {
    String sql = "SELECT COUNT(*) FROM REVIEW WHERE BOOK_ID = ?";
    return jdbcTemplate.queryForObject(sql, Integer.class, bookId);
  }

  // 별점별 리뷰 수 조회
  public int countByBookIdAndRating(Long bookId, int rating) {
    String sql = "SELECT COUNT(*) FROM REVIEW " +
        "WHERE BOOK_ID = ? AND REVIEW_RATING = ?";
    return jdbcTemplate.queryForObject(sql, Integer.class, bookId, rating);
  }

  // ──────────────────────────────────────────
  // ReviewLikes CRUD
  // ──────────────────────────────────────────

  // 좋아요 추가 (복합 PK - 중복 좋아요 DB에서 방지)
  public int insertLike(ReviewLikes reviewLikes) {
    String sql = "INSERT INTO REVIEW_LIKES (REVIEW_ID, MEMBER_ID) VALUES (?, ?)";
    return jdbcTemplate.update(sql,
        reviewLikes.getReviewId(),
        reviewLikes.getMemberId());
  }

  // 좋아요 취소 (복합 PK 기준)
  public int deleteLike(Long reviewId, Long memberId) {
    String sql = "DELETE FROM REVIEW_LIKES WHERE REVIEW_ID = ? AND MEMBER_ID = ?";
    return jdbcTemplate.update(sql, reviewId, memberId);
  }

  // 특정 리뷰 좋아요 수 조회
  public int countLikeByReviewId(Long reviewId) {
    String sql = "SELECT COUNT(*) FROM REVIEW_LIKES WHERE REVIEW_ID = ?";
    return jdbcTemplate.queryForObject(sql, Integer.class, reviewId);
  }

  // 좋아요 여부 조회 (복합 PK 기준)
  public boolean existsLike(Long reviewId, Long memberId) {
    String sql = "SELECT COUNT(*) FROM REVIEW_LIKES " +
        "WHERE REVIEW_ID = ? AND MEMBER_ID = ?";
    int count = jdbcTemplate.queryForObject(sql, Integer.class, reviewId, memberId);
    return count > 0;
  }
}