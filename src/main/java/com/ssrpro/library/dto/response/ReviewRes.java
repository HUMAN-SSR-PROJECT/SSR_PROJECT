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

  public Long reviewId;
  private String reviewComment;
  private Double reviewRating;
  private LocalDateTime reviewCreatedAt;
  private LocalDateTime reviewUpdatedAt;

  private Long likeCount;
  private Boolean isLiked;
  private Long commentCount;

  private String memberNickname; // [2026-05-13-14:32 추가] 엔티티에서 변환
  private String memberImgUrl; // [2026-05-13-14:32 추가] 엔티티에서 변환
}