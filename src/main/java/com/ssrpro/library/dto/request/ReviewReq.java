package com.ssrpro.library.dto.request;

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
}