package com.ssrpro.library.dto.response;

import com.ssrpro.library.dto.entity.ReadBook;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
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
@Valid
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

  public static ReadBookRes of(ReadBook readBook, Member member, Book book) {
    return ReadBookRes.builder()
            .memberId(member.getMemberId())
            .bookId(book.getBookId())
            .bookImg(book.getBookImg())
            .bookGenre(book.getGenre())
            .bookTitle(book.getTitle())
            .bookWriter(book.getWriter())
            .readBookState(readBook.getReadBookState())
            .readBookRating(readBook.getReadBookRating())
            .readBookStart(readBook.getReadBookStart())
            .readBookEnd(readBook.getReadBookEnd())
            .build();
  }
}
