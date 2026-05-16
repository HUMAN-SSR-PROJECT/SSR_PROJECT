package com.ssrpro.library.controller;

import com.ssrpro.library.dto.request.BookDetailReq;
import com.ssrpro.library.dto.request.ReviewReq;
import com.ssrpro.library.dto.security.CustomUser;
import com.ssrpro.library.service.BookService;
import com.ssrpro.library.service.LibraryService;
import com.ssrpro.library.service.ReadBookService;
import com.ssrpro.library.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public String bookDetail(Model model, @PathVariable Long bookId, @ModelAttribute BookDetailReq req, @AuthenticationPrincipal CustomUser customUser){
        // 시큐리티 memberId 가져오기 try-catch() 사용 그리고 없을경우 검색 페이지 리턴
        // 1. 로그인 체크 (혹시 모르니)
        if (customUser == null) {
            return "redirect:/member/login";
        }

        // 2. ID 꺼내기
        Long memberId = customUser.getMemberId();

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
    public String insertReadBook(@PathVariable Long bookId, RedirectAttributes redirectAttributes, @AuthenticationPrincipal CustomUser customUser){
        // 시큐리티 memberId 가져오기 try-catch() 사용 그리고 없을경우 로그인 페이지로
        // 1. 로그인 체크 (혹시 모르니)
        if (customUser == null) {
            return "redirect:/member/login";
        }

        // 2. ID 꺼내기
        Long memberId = customUser.getMemberId();

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
    public String insertReadSoon(@PathVariable Long bookId, RedirectAttributes redirectAttributes, @AuthenticationPrincipal CustomUser customUser){
        // 시큐리티 memberId 가져오기 try-catch() 사용 그리고 없을경우 로그인 페이지로
        // 1. 로그인 체크 (혹시 모르니)
        if (customUser == null) {
            return "redirect:/member/login";
        }

        // 2. ID 꺼내기
        Long memberId = customUser.getMemberId();

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

    // 1. 리뷰 작성
    @PostMapping("/insert")
    public String insert(@ModelAttribute ReviewReq req,
                         @AuthenticationPrincipal CustomUser customUser,
                         RedirectAttributes rttr) {

        int result = reviewService.insert(req, customUser.getMemberId());

        if (result > 0) {
            rttr.addFlashAttribute("msg", "리뷰가 등록되었습니다.");
        } else {
            rttr.addFlashAttribute("msg", "이미 리뷰를 작성하셨습니다.");
        }

        // 작성 후 해당 도서 상세 페이지로 다시 이동
        return "redirect:/book/detail/" + req.getBookId();
    }

    // 2. 리뷰 수정
    @PostMapping("/update")
    public String update(@ModelAttribute ReviewReq req,
                         @AuthenticationPrincipal CustomUser customUser,
                         RedirectAttributes rttr) {

        int result = reviewService.update(req, customUser.getMemberId());

        if (result > 0) {
            rttr.addFlashAttribute("msg", "리뷰가 수정되었습니다.");
        } else {
            rttr.addFlashAttribute("msg", "수정 권한이 없습니다.");
        }

        return "redirect:/book/detail/" + req.getBookId();
    }

    // 3. 리뷰 삭제 (본인)
    // HTML 폼에서는 DELETE를 쓰기 복잡하므로 POST로 처리하는 경우가 많습니다.
    @PostMapping("/delete/{reviewId}")
    public String delete(@PathVariable Long reviewId,
                         @RequestParam Long bookId,
                         @AuthenticationPrincipal CustomUser customUser,
                         RedirectAttributes rttr) {

        int result = reviewService.delete(reviewId, customUser.getMemberId());

        if (result > 0) {
            rttr.addFlashAttribute("msg", "리뷰가 삭제되었습니다.");
        } else {
            rttr.addFlashAttribute("msg", "삭제 권한이 없습니다.");
        }

        return "redirect:/book/detail/" + bookId;
    }

    // 4. 관리자 리뷰 삭제
    @PostMapping("/admin/delete/{reviewId}")
    @PreAuthorize("hasRole('ADMIN')")
    public String adminDelete(@PathVariable Long reviewId,
                              @RequestParam Long bookId,
                              RedirectAttributes rttr) {

        reviewService.adminDelete(reviewId);
        rttr.addFlashAttribute("msg", "관리자 권한으로 삭제되었습니다.");

        return "redirect:/book/detail/" + bookId;
    }

    // 리뷰 도움이 되요 토글
    @PostMapping("/like/{reviewId}")
    public String toggleLike(@PathVariable Long reviewId,
                             @RequestParam Long bookId,
                             @AuthenticationPrincipal CustomUser customUser,
                             RedirectAttributes rttr) {

        // 1. 로그인 여부 확인 (시큐리티가 막아주지만 안전장치로 추가)
        if (customUser == null) {
            rttr.addFlashAttribute("msg", "로그인이 필요한 서비스입니다.");
            return "redirect:/member/login";
        }

        // 2. 서비스 호출 (이미 있는 토글 로직 실행)
        // 좋아요가 있으면 삭제, 없으면 추가를 서비스에서 알아서 처리함
        reviewService.toggleLike(reviewId, customUser.getMemberId());

        // 3. 다시 도서 상세 페이지로 리다이렉트 (새로고침 효과)
        return "redirect:/book/detail/" + bookId;
    }
}
