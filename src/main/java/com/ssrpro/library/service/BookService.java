package com.ssrpro.library.service;


import com.ssrpro.library.dao.BookDao;
import com.ssrpro.library.dto.entity.Book;
import com.ssrpro.library.dto.request.BookSearchReq;
import com.ssrpro.library.dto.response.BookRes;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BookDao bookDao;

    // 도서 통합 검색
    public List<BookRes> searchBooks(BookSearchReq req) {
        List<Book> books = bookDao.findByKeyword(req.getKeyword());
        if (books.isEmpty()) {
            return new ArrayList<>();
        }
        return books.stream()
                .map(book -> BookRes.of(book, new ArrayList<>()))
                .collect(Collectors.toList());
    }
    // 도서 목록 조회
    public List<BookRes> findAllBooks() {
        List<Book> books = bookDao.findAll();
        return books.stream()
                .map(book -> BookRes.of(book, new ArrayList<>()))
                .collect(Collectors.toList());
    }
    // 도서 상세 조회
    public BookRes getBookDetail(Long bookId) {
        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new RuntimeException("도서를 찾을 수 없습니다."));
        //하드코딩
        Long currentMemberId = 1L;
        // 해당 도서의 소장 도서관 정보 api로 불러옴
        // List<String> libraryCodes = libraryApi.search(book.getBookIsbn());
        List<String> tempLibraryCodes = List.of("LIB001", "LIB002"); // 하드코딩

        return BookRes.of(book, tempLibraryCodes);
    }
    // 도서 저장
    public boolean registerBook(String isbn) {
        // 1. 외부 API를 통해 도서의 모든 상세 정보를 가져옴 // api로 불러옴
        // (제목, 저자, 출판사, 장르, 발행일, 페이지수, 줄거리 등)
        // Book apiBook = bookExternalApi.fetch(isbn);

        // 2. 파이어베이스 이미지 처리
        String firebaseUrl = "https://firebasestorage.../" + isbn + ".png";

        // 3. 시큐리티 미적용으로 인한 하드코딩
        boolean isAdmin = true;

        // 4. Book 객체 생성 (BookDao.save의 SQL 순서에 맞게 모든 정보 입력)
        Book newBook = Book.builder()
                .bookIsbn(isbn)
                .bookImg(firebaseUrl)
                .bookTitle("외부 API 제목")
                .bookWriter("외부 API 저자")
                .bookCompany("외부 API 출판사")
                .bookGenre("소설")
                .bookYear(LocalDate.now())
                .bookPages(300)
                .bookStory("이 책의 줄거리입니다.")
                .build();

        if (isAdmin) {
            return bookDao.save(newBook);
        }
        return false;
    }
}
