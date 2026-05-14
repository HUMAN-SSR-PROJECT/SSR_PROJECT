package com.ssrpro.library.service;

import com.ssrpro.library.dao.ReviewDao;
import com.ssrpro.library.dto.entity.Review;
import com.ssrpro.library.dto.entity.ReviewLikes;
import com.ssrpro.library.dto.request.ReviewReq;
import com.ssrpro.library.dto.response.ReviewRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

// Spring Bean으로 등록 - Service 클래스임을 명시
@Service
// ReviewDao 의존성 주입을 위한 생성자 자동 생성
@RequiredArgsConstructor
public class ReviewService {

  // DB 접근을 위한 ReviewDao 주입
  private final ReviewDao reviewDao;

  // 리뷰 작성
  // ReviewReq → toEntity()로 Review 엔티티 변환 후 DAO에 전달
  // memberId는 세션에서 가져온 로그인한 회원 ID
  public int insert(ReviewReq req, Long memberId) {

    // 한 회원이 한 책에 리뷰 1개만 작성 가능 (DB UNIQUE 제약과 이중 체크)
    // findByBookId로 해당 책의 리뷰 중 본인 리뷰가 있는지 확인
    boolean alreadyExists = reviewDao.findByBookId(req.getBookId())
        .stream()
        .anyMatch(r -> r.getMemberId().equals(memberId));

    if (alreadyExists) {
      // 이미 리뷰가 있으면 0 반환 (작성 실패)
      return 0;
    }

    // Req → 엔티티 변환 후 INSERT
    return reviewDao.insert(req.toEntity(memberId));
  }

  // 리뷰 수정
  // DAO의 update는 MEMBER_ID 조건으로 본인 리뷰만 수정
  // 반환값이 0이면 본인 리뷰가 아니거나 존재하지 않는 리뷰
  public int update(ReviewReq req, Long memberId) {

    // Req → 엔티티 변환 후 UPDATE
    // memberId 조건으로 본인 리뷰만 수정 가능
    return reviewDao.update(req.toEntity(memberId));
  }

  // 리뷰 삭제 (본인 확인)
  // DAO의 delete는 MEMBER_ID 조건으로 본인 리뷰만 삭제
  public int delete(Long reviewId, Long memberId) {

    // REVIEW_ID + MEMBER_ID 조건으로 본인 리뷰만 삭제
    return reviewDao.delete(reviewId, memberId);
  }

  // 관리자 리뷰 삭제 (조건 없음)
  // 관리자 여부는 Controller에서 확인 후 호출
  public int adminDelete(Long reviewId) {

    // REVIEW_ID 조건만으로 삭제
    return reviewDao.adminDelete(reviewId);
  }

  // 도서별 리뷰 목록 조회
  // Review 엔티티 → ReviewRes 변환
  // likeCount, isLiked는 DAO에서 별도 조회 후 세팅
  public List<ReviewRes> findByBookId(Long bookId, Long memberId) {

    // DAO에서 Review 엔티티 목록 조회 (MEMBERS JOIN 포함)
    List<Review> reviews = reviewDao.findByBookId(bookId);

    // 엔티티 → ReviewRes 변환
    return reviews.stream()
        .map(review -> ReviewRes.builder()
            // 리뷰 기본 정보
            .reviewId(review.getReviewId())
            .reviewComment(review.getReviewComment())
            .reviewRating(review.getReviewRating())
            .reviewCreatedAt(review.getReviewCreatedAt())
            .reviewUpdatedAt(review.getReviewUpdatedAt())
            // MEMBERS JOIN으로 가져온 작성자 정보
            .memberNickname(review.getMemberNickname())
            .memberImgUrl(review.getMemberImgUrl())
            // 좋아요 수 - COUNT 쿼리로 조회
            .likeCount((long) reviewDao.countLikeByReviewId(review.getReviewId()))
            // 로그인한 회원의 좋아요 여부
            .isLiked(reviewDao.existsLike(review.getReviewId(), memberId))
            .build())
        // stream()으로 가공된 ReviewRes 객체들을 List로 수집하여 반환
        .collect(Collectors.toList());
  }

  // 전체 리뷰 수 조회
  public int countByBookId(Long bookId) {
    return reviewDao.countByBookId(bookId);
  }

  // 별점별 리뷰 수 조회
  public int countByBookIdAndRating(Long bookId, int rating) {
    return reviewDao.countByBookIdAndRating(bookId, rating);
  }

  // 별점별 퍼센트 계산 (1~5점)
  // DAO에서 COUNT로 가져온 값을 퍼센트로 변환
  public Map<Integer, Double> getRatingPercent(Long bookId) {

    // 전체 리뷰 수
    int total = reviewDao.countByBookId(bookId);

    Map<Integer, Double> ratingPercent = new LinkedHashMap<>();

    for (int i = 1; i <= 5; i++) {
      // 별점별 리뷰 수
      int count = reviewDao.countByBookIdAndRating(bookId, i);
      // 퍼센트 계산 (전체 0이면 0% 처리 - 0으로 나누기 방지)
      double percent = total > 0 ? (double) count / total * 100 : 0;
      // 소수점 없이 반올림 (예: 62%)
      ratingPercent.put(i, (double) Math.round(percent));
    }

    return ratingPercent;
  }

  // 좋아요 토글 (추가/취소 자동 전환)
  public int toggleLike(Long reviewId, Long memberId) {

    // 이미 좋아요 눌렀으면 → 취소
    if (reviewDao.existsLike(reviewId, memberId)) {
      return reviewDao.deleteLike(reviewId, memberId);
    }

    // 좋아요 안 눌렀으면 → 추가
    ReviewLikes reviewLikes = ReviewLikes.builder()
        .reviewId(reviewId)
        .memberId(memberId)
        .build();

    return reviewDao.insertLike(reviewLikes);
  }

}