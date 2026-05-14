package com.ssrpro.library.controller;

import com.ssrpro.library.dto.request.BookDetailReq;
import com.ssrpro.library.dto.response.BookRes;
import com.ssrpro.library.service.BookService;
import com.ssrpro.library.service.LibraryService;
import com.ssrpro.library.service.ReadBookService;
import com.ssrpro.library.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/book/detail")
@RequiredArgsConstructor
public class BookDetailController {
    private final BookService bookService;
    private final ReviewService reviewService;
    private final LibraryService libraryService;
    private final ReadBookService readBookService;

    // 상세 페이지 진입
    @GetMapping("/{bookId}")
    public String bookDetail(Model model, @PathVariable Long bookId, @ModelAttribute BookDetailReq req){
        // 시큐리티 memberId 가져오기 try-catch() 사용 그리고 없을경우 검색 페이지 리턴

        try{
            model.addAttribute("book", bookService.getBookDetail(bookId));
        }catch(Exception e){
            model.addAttribute("bookError", e.getMessage());
        }

        try{
            model.addAttribute("review", reviewService.findByBookId(bookId, memberId));
        }catch (Exception e){
            model.addAttribute("reviewError", e.getMessage());
        }

        try{
            model.addAttribute("library", libraryService.findByLibraryCode(req));
        }catch(Exception e){
            model.addAttribute("libraryError", e.getMessage());
        }

        return "book/detail";
    }
    // 읽는중 추가 - 3단계 상태 완독시 변경불가
    @PostMapping("/{bookId}/readbook")
    public String insertReadBook(@PathVariable Long bookId, RedirectAttributes redirectAttributes){
        // 시큐리티 memberId 가져오기 try-catch() 사용 그리고 없을경우 로그인 페이지로
        try{
            boolean rst = readBookService.addToReading(memberId, bookId);
            if(!rst){
                redirectAttributes.addFlashAttribute("readBookError", "입력중 에러 발생");
                return "redirect:book/detail" + bookId;
            }
            return "redirect:book/detail" + bookId;
        }catch(Exception e){
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:book/detail" + bookId;
        }
    }
    // 읽을책 추가 - 즐겨찾기
    @PostMapping("/{bookId}/readSoon")
    public String insertReadSoon(@PathVariable Long bookId, RedirectAttributes redirectAttributes){
        // 시큐리티 memberId 가져오기 try-catch() 사용 그리고 없을경우 로그인 페이지로
        try{
            boolean rst = readBookService.addToReadSoon(memberId, bookId);
            if(!rst){
                redirectAttributes.addFlashAttribute("readBookError", "입력중 에러 발생");
                return "redirect:book/detail" + bookId;
            }
            return "redirect:book/detail" + bookId;
        }catch(Exception e){
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:book/detail" + bookId;
        }
    }
}
