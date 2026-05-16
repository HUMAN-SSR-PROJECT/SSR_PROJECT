package com.ssrpro.library.controller;


import org.springframework.ui.Model;
import com.ssrpro.library.dto.request.BookSearchReq;
import com.ssrpro.library.dto.response.BookRes;
import com.ssrpro.library.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/book")
@RequiredArgsConstructor
public class BookController {
    private final BookService bookService;

    // 도서 통합 검색 페이지
    @GetMapping("/search")
    public String searchPage(Model model) {

        model.addAttribute("searchReq", new BookSearchReq());
        return "book/search";
    }
    // 도서 검색 실행
    @GetMapping("/search/execute")
    public String executeSearch(@ModelAttribute("searchReq") BookSearchReq req, Model model) {
        // 서비스에서 검색 결과 리스트를 가져옴
        List<BookRes> searchResults = bookService.searchBooks(req);

        // 검색 결과 리스트
        model.addAttribute("searchResults", searchResults);

        // 검색 결과 개수
        model.addAttribute("count", searchResults.size());

        // 결과 없을 때
        model.addAttribute("isEmpty", searchResults.isEmpty());

        // 키워드, 지역 정보 유지
        model.addAttribute("keyword", req.getKeyword());
        model.addAttribute("city", req.getCity());
        model.addAttribute("district", req.getDistrict());

        return "book/search";
    }
}
