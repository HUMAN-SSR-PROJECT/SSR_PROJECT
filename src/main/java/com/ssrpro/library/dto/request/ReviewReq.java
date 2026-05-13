package com.ssrpro.library.dto.request;

import com.ssrpro.library.dto.entity.Review;

import lombok.*;

@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReq {
  private Long reviewId;
  private Long bookId;
  private double rating;
  private String comment;

  public Review toEntity(Long memberId) {
    return Review.builder()
        .reviewId(this.reviewId)
        .bookId(this.bookId)
        .memberId(memberId)
        .reviewRating(this.rating)
        .reviewComment(this.comment)
        .build();
  }
}