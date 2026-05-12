package com.ssrpro.library.dto.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReadBookRes {
  private Long memberId;
  private Long bookId;
  private String bookImg;
  private String bookGenre;
  private String bookTitle;
  private String bookWriter;
  private String readBookState;
  private Double readBookRating;
  private LocalDateTime readBookStart;
  private LocalDateTime readBookEnd;
}
