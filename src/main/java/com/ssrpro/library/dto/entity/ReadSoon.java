package com.ssrpro.library.dto.entity;

import java.time.LocalDateTime;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReadSoon {
  private Long memberId;
  private Long bookId;
  private LocalDateTime readSoonCreatedAt;
  private LocalDateTime readSoonUpdatedAt;
}
