package com.ssrpro.library.controller;

import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.BookDetailReq;
import com.ssrpro.library.dto.request.ReviewReq;
import com.ssrpro.library.dto.response.BookRes;
import com.ssrpro.library.dto.response.LibraryRes;
import com.ssrpro.library.dto.security.CustomUser;
import com.ssrpro.library.service.BookService;
import com.ssrpro.library.service.LibraryService;
import com.ssrpro.library.service.ReadBookService;
import com.ssrpro.library.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(BookDetailController.class)
class BookDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BookService bookService;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private LibraryService libraryService;

    @MockBean
    private ReadBookService readBookService;

    private BookRes sampleBook;

    @BeforeEach
    void setUp() {
        sampleBook = BookRes.builder()
                .bookId(1L)
                .bookTitle("테스트 도서")
                .bookWriter("저자")
                .bookIsbn("9788936434267")
                .bookRating(4.0)
                .build();
    }

    private CustomUser testUser() {
        return new CustomUser(Members.builder()
                .id(1L)
                .email("user@test.com")
                .password("pw")
                .nickname("테스트")
                .name("테스트")
                .birth(LocalDate.of(1990, 1, 1))
                .rule("N")
                .build());
    }

    private CustomUser adminUser() {
        return new CustomUser(Members.builder()
                .id(2L)
                .email("admin@test.com")
                .password("pw")
                .nickname("관리자")
                .name("관리자")
                .birth(LocalDate.of(1990, 1, 1))
                .rule("Y")
                .build());
    }

    @Test
    @DisplayName("상세 GET — book/detail 뷰와 일괄 데이터 로드")
    void bookDetail_get() throws Exception {
        when(bookService.getBookDetail(1L)).thenReturn(sampleBook);
        when(reviewService.findByBookId(eq(1L), any())).thenReturn(List.of());
        when(libraryService.findLibrariesForBookDetail(
                        any(BookDetailReq.class), eq("9788936434267"), any()))
                .thenReturn(new LibraryService.BookDetailLibrariesResult(
                        List.of(LibraryRes.builder().libraryName("테스트 도서관").build()),
                        null,
                        null));

        mockMvc.perform(get("/book/detail/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("book/detail"));
    }

    @Test
    @DisplayName("검색에서 상세 POST — libraryCodes flash 후 redirect")
    void bookDetailFromSearch_post() throws Exception {
        mockMvc.perform(post("/book/detail/1")
                        .param("libraryCodes", "111003", "111004")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/book/detail/1"))
                .andExpect(flash().attributeExists("bookDetailReq"));
    }

    @Test
    @DisplayName("독서 기록 POST — libraryCodes flash 유지")
    void insertReadBook_preservesLibraryCodes() throws Exception {
        when(readBookService.readingState(1L, 1L)).thenReturn(1);
        when(readBookService.addToReading(1L, 1L)).thenReturn(true);

        mockMvc.perform(post("/book/detail/1/readbook")
                        .param("libraryCodes", "111003")
                        .with(user(testUser()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/book/detail/1"))
                .andExpect(flash().attributeExists("bookDetailReq"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    @DisplayName("리뷰 등록 POST — detailTab reviews flash")
    void insertReview_flashesDetailTab() throws Exception {
        when(reviewService.insert(any(ReviewReq.class), eq(1L))).thenReturn(1);

        mockMvc.perform(post("/book/detail/insert")
                        .param("bookId", "1")
                        .param("rating", "5")
                        .param("comment", "정말 좋은 책이었습니다.")
                        .param("libraryCodes", "111003")
                        .with(user(testUser()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/book/detail/1"))
                .andExpect(flash().attribute("detailTab", "reviews"))
                .andExpect(flash().attributeExists("bookDetailReq"));
    }

    @Test
    @DisplayName("비로그인 독서 기록 POST — 로그인 redirect")
    void insertReadBook_requiresLogin() throws Exception {
        mockMvc.perform(post("/book/detail/1/readbook").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/member/login?needLogin=true"));
    }

    @Test
    @DisplayName("관리자 리뷰 삭제 POST")
    void adminDeleteReview() throws Exception {
        mockMvc.perform(post("/book/detail/admin/delete/10")
                        .param("bookId", "1")
                        .param("libraryCodes", "111003")
                        .with(user(adminUser()))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/book/detail/1"))
                .andExpect(flash().attribute("detailTab", "reviews"));

        verify(reviewService).adminDelete(10L);
    }
}
