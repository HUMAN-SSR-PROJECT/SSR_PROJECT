package com.ssrpro.library.dto.response;

import com.ssrpro.library.dto.entity.Book;
import com.ssrpro.library.dto.entity.ReadSoon;

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
public class ReadSoonRes {
  private Long bookId;
  private String bookImg;
  private String bookGenre;
  private String bookTitle;
  private String bookWriter;
  private Double bookRating;

  public static ReadSoonRes of(ReadSoon readSoon, Book book) {
    return ReadSoonRes.builder()
            .bookId(readSoon.getBookId())
            .bookImg(book.getBookImg())
            .bookGenre(book.getBookGenre())
            .bookTitle(book.getBookTitle())
            .bookWriter(book.getBookWriter())
            .bookRating(book.getBookRating())
            .build();
  }
}
