package com.ssrpro.library.dto.entity;

import lombok.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Valid
public class Review {

  private Long reviewId;
  private Long bookId;
  private Long memberId;
  private String reviewComment;
  private Double reviewRating;

  private LocalDateTime reviewCreatedAt;
  private LocalDateTime reviewUpdatedAt;
}