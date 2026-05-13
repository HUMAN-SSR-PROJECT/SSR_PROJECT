package com.ssrpro.library.dto.entity;

import lombok.*;
import jakarta.validation.Valid;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Valid
public class ReviewLikes {

  private Long memberId;
  private Long reviewId;
  private LocalDateTime reviewLikesCreatedAt;
  private LocalDateTime reviewLikesUpdatedAt;

}