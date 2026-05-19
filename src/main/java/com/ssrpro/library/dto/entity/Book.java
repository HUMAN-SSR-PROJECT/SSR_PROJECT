package com.ssrpro.library.dto.entity;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Book {
    // PK
    private Long bookId;
    // 책 이미지
    private String bookImg;
    // 제목
    private String bookTitle;
    // 저자
    private String bookWriter;
    // 출판사
    private String bookCompany;
    // 장르
    private String bookGenre;
    // 발행 연도
    private LocalDate bookYear;
    // ISBN
    private String bookIsbn;
    // 페이지 수
    private int bookPages;
    // 줄거리
    private String bookStory;
    // 별점
    private double bookRating;
    // 등록일
    private LocalDateTime bookCreatedAt;
    // 수정일
    private LocalDateTime bookUpdatedAt;
    // 도서관 코드 리스트
    private List<String> libraryCodes;

}