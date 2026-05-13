package com.ssrpro.library.dto.response;

import lombok.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Valid
public class ReviewRes {

  private Long reviewId;
  private String reviewComment;
  private Double reviewRating;
  private LocalDateTime reviewCreatedAt;
  private LocalDateTime reviewUpdatedAt;

  private Long bookId;
  private String bookTitle;
  private String bookAuthor;
  private String bookPublisher;
  private String bookDescription;
  private String bookImageUrl;

  private Long memberId;
  private String memberName;
  private String memberNickname;
  private String memberEmail;
  private String memberProfileImage;
  private String memberRole;

  private Long likeCount;
  private Boolean isLiked;
  private Long commentCount;
  private Double bookAverageRating;
  private Long bookTotalReviews;
}