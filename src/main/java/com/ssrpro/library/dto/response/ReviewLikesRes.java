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
public class ReviewLikesRes {

  private Long memberId;
  private Long reviewId;
  private LocalDateTime reviewLikesCreatedAt;
  private LocalDateTime reviewLikesUpdatedAt;

}