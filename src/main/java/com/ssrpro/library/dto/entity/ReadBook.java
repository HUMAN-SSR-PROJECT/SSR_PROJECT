package com.ssrpro.library.dto.entity;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReadBook {
  private Long memberId;
  private Long bookId;
  private String readBookState;
  private LocalDateTime readBookStart;
  private LocalDateTime readBookEnd;
  private String readBookMemo;
  private Double readBookRating;
  private LocalDateTime readBookCreatedAt;
  private LocalDateTime readBookUpdatedAt;
}
