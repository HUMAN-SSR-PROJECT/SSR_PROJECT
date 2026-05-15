package com.ssrpro.library.controller;

import com.ssrpro.library.constant.MemberStatus;
import com.ssrpro.library.dto.request.BookSearchReq;
import com.ssrpro.library.service.BookService;
import com.ssrpro.library.service.MemberService;
import com.ssrpro.library.service.ReadBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;

/**
 * AdminController
 * - 독서 통계 및 도서 관리 통합 (/admin/books, /admin/stats)
 * - 회원 관리 통합 검색 및 상태 관리 (state 변수 사용)
 */
@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

  private final BookService bookService;
  private final MemberService memberService;
  private final ReadBookService readBookService;

  /**
   * 관리자 대시보드
   * 전체 도서 수와 전체 회원 수만 깔끔하게 표시합니다.
   */
  @GetMapping("/dashboard")
  public String dashboard(Model model) {
    model.addAttribute("totalBooks", bookService.countAllBooks());
    model.addAttribute("totalMembers", memberService.countAllMembers());

    return "admin/dashboard";
  }

  /**
   * [통합] 도서 목록 및 독서 통계
   */
  @GetMapping({ "/books", "/stats" })
  public String bookList(
      @RequestParam(required = false, defaultValue = "") String keyword,
      Model model) {

    // 도서 목록 및 검색
    BookSearchReq req = BookSearchReq.builder()
        .keyword(keyword.trim())
        .build();
    model.addAttribute("books", bookService.searchBooks(req));
    model.addAttribute("keyword", keyword.trim());

    // 통합된 독서 통계 데이터
    model.addAttribute("globalStats", readBookService.getGlobalReadingStats());

    return "admin/book-manage";
  }

  /**
   * 회원 목록 조회
   * searchType을 버리고 keyword 하나로 통합 검색을 수행합니다.
   */
  @GetMapping("/members")
  public String memberList(
      @RequestParam(required = false, defaultValue = "") String keyword,
      Model model) {

    // 상단 현황은 요청하신 대로 전체 회원수만 표시합니다.
    model.addAttribute("totalCount", memberService.countAllMembers());

    // 통합 검색 (Service 내부에서 OR 조건으로 처리한다고 가정)
    model.addAttribute("members", memberService.searchMembers(keyword.trim()));
    model.addAttribute("keyword", keyword.trim());

    return "admin/member-list";
  }

  /**
   * 회원 상태 변경
   */
  @PostMapping("/members/{id}/update-status")
  public String updateMemberStatus(
      @PathVariable @Positive Long id,
      @RequestParam MemberStatus state) {

    memberService.updateStatus(id, state);
    return "redirect:/admin/members";
  }

  /**
   * 도서 삭제
   */
  @PostMapping("/books/{id}/delete")
  public String deleteBook(@PathVariable @Positive Long id) {
    bookService.deleteBook(id);
    return "redirect:/admin/books";
  }
}