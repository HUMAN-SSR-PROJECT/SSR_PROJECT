package com.ssrpro.library.dto.response;


import com.ssrpro.library.dto.entity.Book;
import jakarta.validation.Valid;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@ToString
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Valid
public class BookRes {
    private Long bookId;
    private String bookImg;
    private String bookTitle;
    private String bookWriter;
    private String bookCompany;
    private String bookGenre;
    private LocalDate bookYear;
    private String bookIsbn;
    private int bookPages;
    private String bookStory;
    private double bookRating;
    private LocalDateTime bookCreatedAt;
    private LocalDateTime bookUpdatedAt;

    public static BookRes of(Book book) {
        return BookRes.builder()
                .bookId(book.getBookId())
                .bookImg(book.getBookImg())
                .bookTitle(book.getBookTitle())
                .bookWriter(book.getBookWriter())
                .bookCompany(book.getBookCompany())
                .bookGenre(book.getBookGenre())
                .bookYear(book.getBookYear())
                .bookIsbn(book.getBookIsbn())
                .bookPages(book.getBookPages())
                .bookStory(book.getBookStory())
                .bookRating(book.getBookRating())
                .bookCreatedAt(book.getBookCreatedAt())
                .bookUpdatedAt(book.getBookUpdatedAt())
                .build();
    }
}

