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
public class ReadSoon {
  private Long memberId;
  private Long bookId;
  private LocalDateTime readSoonCreatedAt;
  private LocalDateTime readSoonUpdatedAt;
}
