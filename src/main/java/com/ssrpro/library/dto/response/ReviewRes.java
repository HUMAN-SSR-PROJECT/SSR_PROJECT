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

  private String reviewComment;
  private Double reviewRating;
  private LocalDateTime reviewCreatedAt;
  private LocalDateTime reviewUpdatedAt;

  private Long likeCount;
  private Boolean isLiked;
  private Long commentCount;
}