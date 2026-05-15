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
 *
 * 관리자 전용 기능을 담당하는 MVC Controller
 *
 * Controller는 “요청/응답”만 담당하고
 * 비즈니스 로직은 Service에서 처리하는 구조 유지
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
   *
   * [개선 포인트]
   * - Map 대신 DTO로 변경 가능하지만 현재는 단순 통계라 유지
   */
  @GetMapping("/dashboard")
  public String dashboard(Model model) {

    model.addAttribute("totalBooks", bookService.countAllBooks());
    model.addAttribute("totalMembers", memberService.countAllMembers());

    return "admin/dashboard";
  }

  /**
   * 회원 목록 조회
   */
  @GetMapping("/members")
  public String memberList(
      @RequestParam(required = false) String searchType,
      @RequestParam(required = false, defaultValue = "") String keyword,
      Model model) {

    model.addAttribute(
        "members",
        memberService.searchMembers(searchType, keyword.trim()));

    return "admin/member-list";
  }

  /**
   * 회원 상태 변경
   *
   * [개선 포인트]
   * - String → Enum(MemberStatus) 사용 권장
   * - 잘못된 상태값 입력 방지
   */
  @PostMapping("/members/{id}/update-status")
  public String updateMemberStatus(
      @PathVariable @Positive Long id,
      @RequestParam MemberStatus status) {

    memberService.updateStatus(id, status);

    return "redirect:/admin/members";
  }

  /**
   * 도서 목록 조회
   */
  @GetMapping("/books")
  public String bookList(
      @RequestParam(required = false, defaultValue = "") String keyword,
      Model model) {

    BookSearchReq req = BookSearchReq.builder()
        .keyword(keyword.trim())
        .build();

    model.addAttribute("books", bookService.searchBooks(req));

    return "admin/book-list";
  }

  /**
   * 도서 삭제
   *
   * [주의]
   * Soft Delete[데이터를 “삭제하지 않고 삭제된 것처럼 처리”] 확장 가능 구조
   */
  @PostMapping("/books/{id}/delete")
  public String deleteBook(@PathVariable @Positive Long id) {

    bookService.deleteBook(id);

    return "redirect:/admin/books";
  }

  /**
   * 독서 통계 조회
   *
   * [설계]
   * Controller는 데이터 전달만 담당
   * 통계 계산은 Service에서 처리
   */
  @GetMapping("/stats")
  public String readingStats(Model model) {

    model.addAttribute(
        "globalStats",
        readBookService.getGlobalReadingStats());

    return "admin/stats";
  }
}