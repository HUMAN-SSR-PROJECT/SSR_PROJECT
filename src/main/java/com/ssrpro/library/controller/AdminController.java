package com.ssrpro.library.controller;

import com.ssrpro.library.dto.request.BookSearchReq;
import com.ssrpro.library.service.BookService;
import com.ssrpro.library.service.MemberService;
import com.ssrpro.library.service.ReadBookService;
import com.ssrpro.library.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;

/**
 * AdminController
 * [역할] 관리자 전용 대시보드, 도서 관리, 회원 관리를 담당하는 컨트롤러입니다.
 * [권한] 'ADMIN' 권한을 가진 사용자만 접근 가능합니다.
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
  private final LibraryService libraryService;

  /**
   * [1] 관리자 대시보드
   */
  @GetMapping("/dashboard")
  public String dashboard(Model model) {
    // 상단 통계
    model.addAttribute("totalMembers", memberService.getTotalCount());
    model.addAttribute("totalLibraries", libraryService.countAllLibrary());
    model.addAttribute("totalBooks", bookService.countAllBooks());

    // 하단 최근 내역 (최신 가입 회원 및 최근 등록 도서 10개 씩 가져오기)
    model.addAttribute("recentMembers", memberService.findRecentMembers(10));
    model.addAttribute("recentBooks", bookService.getRecentBooks(10)); // getRecentBooks() 적용

    return "admin/dashboard";
  }

  /**
   * [2] 도서 관리 및 독서 통계 통합 페이지
   */
  @GetMapping("/books")
  public String bookManagement(
      @RequestParam(required = false, defaultValue = "") String keyword,
      Model model) {

    BookSearchReq req = BookSearchReq.builder()
        .keyword(keyword.trim())
        .build();
    model.addAttribute("books", bookService.searchBooks(req));
    model.addAttribute("keyword", keyword.trim());

    model.addAttribute("totalBooksCount", bookService.countAllBooks());
    model.addAttribute("totalGenres", bookService.countGenreTypes());
    model.addAttribute("mostCommonGenre", bookService.findMostCommonGenre());

    model.addAttribute("globalStats", readBookService.getGlobalReadingStats());

    return "admin/book-manage";
  }

  /**
   * [3] 도서 삭제 처리
   */
  @PostMapping("/books/{id}/delete")
  public String deleteBook(@PathVariable @Positive Long id) {
    bookService.deleteById(id);
    return "redirect:/admin/books";
  }

  /**
   * [4] 회원 관리 목록 페이지
   */
  @GetMapping("/members")
  public String memberList(
      @RequestParam(required = false, defaultValue = "") String keyword,
      Model model) {

    model.addAttribute("totalCount", memberService.getTotalCount());
    model.addAttribute("members", memberService.getAllMembers(keyword.trim()));
    model.addAttribute("keyword", keyword.trim());

    return "admin/member-list";
  }

  /**
   * [5] 회원 상태 수정 처리
   * - String state를 사용하여 Enum 없이 직접 상태 문자열을 처리합니다.
   */
  @PostMapping("/members/{id}/update-status")
  public String updateMemberStatus(
      @PathVariable @Positive Long id,
      @RequestParam String state) {

    memberService.changeMemberState(id, state);
    return "redirect:/admin/members";
  }

  /**
   * [6] 회원 삭제 처리
   */
  @PostMapping("/members/{id}/delete")
  public String deleteMember(@PathVariable @Positive Long id) {
    memberService.removeMember(id);
    return "redirect:/admin/members";
  }
}