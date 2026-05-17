package com.ssrpro.library.controller;

import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.response.BookRes;
import com.ssrpro.library.dto.response.PageResult;
import com.ssrpro.library.service.BookService;
import com.ssrpro.library.service.MemberService;
import com.ssrpro.library.service.LibraryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.Positive;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

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
  private final LibraryService libraryService;

  /**
   * [1] 관리자 대시보드
   */
  private void addAdminLayout(Model model, String activeNav) {
    model.addAttribute("mainClass", "site-main--fluid");
    model.addAttribute("activeNav", activeNav);
  }

  private static String redirectWithKeyword(String path, String keyword, int page) {
    StringBuilder url = new StringBuilder("redirect:").append(path);
    boolean hasQuery = false;
    if (keyword != null && !keyword.isBlank()) {
      url.append(hasQuery ? "&" : "?")
          .append("keyword=")
          .append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
      hasQuery = true;
    }
    if (page > 1) {
      url.append(hasQuery ? "&" : "?").append("page=").append(page);
    }
    return url.toString();
  }

  @GetMapping("/dashboard")
  public String dashboard(Model model) {
    addAdminLayout(model, "dashboard");
    // 상단 통계
    model.addAttribute("totalMembers", memberService.getTotalCount());
    model.addAttribute("totalLibraries", libraryService.countAllLibrary());
    model.addAttribute("totalBooks", bookService.countAllBooks());

    // 하단 최근 내역 (최신 가입 회원 및 최근 등록 도서 10개 씩 가져오기)
    model.addAttribute("recentMembers", memberService.getRecentMembers());
    model.addAttribute("recentBooks", bookService.getRecentBooks()); // getRecentBooks() 적용

    return "admin/dashboard";
  }

  /**
   * [2] 도서 관리 및 독서 통계 통합 페이지
   */
  @GetMapping("/books")
  public String bookManagement(
      @RequestParam(required = false, defaultValue = "") String keyword,
      @RequestParam(defaultValue = "1") int page,
      Model model) {

    addAdminLayout(model, "books");
    String trimmed = keyword == null ? "" : keyword.trim();
    PageResult<BookRes> bookPage = bookService.findBooksForAdmin(trimmed, page);
    if (bookPage.getTotalPages() > 0 && page > bookPage.getTotalPages()) {
      bookPage = bookService.findBooksForAdmin(trimmed, bookPage.getTotalPages());
    }
    model.addAttribute("books", bookPage.getContent());
    model.addAttribute("pageResult", bookPage);
    model.addAttribute("listCount", bookPage.getTotalItems());
    model.addAttribute("keyword", trimmed);

    model.addAttribute("totalBooksCount", bookService.countAllBooks());
    model.addAttribute("totalGenres", bookService.countGenreTypes());
    model.addAttribute("mostCommonGenre", bookService.findMostCommonGenre());
    return "admin/book-manage";
  }

  /**
   * [3] 도서 삭제 처리
   */
  @PostMapping("/books/{id}/delete")
  public String deleteBook(
      @PathVariable @Positive Long id,
      @RequestParam(required = false, defaultValue = "") String keyword,
      @RequestParam(defaultValue = "1") int page) {
    bookService.deleteById(id);
    return redirectWithKeyword("/admin/books", keyword, page);
  }

  /**
   * [4] 회원 관리 목록 페이지
   */
  @GetMapping("/members")
  public String memberList(
      @RequestParam(required = false, defaultValue = "") String keyword,
      @RequestParam(defaultValue = "1") int page,
      Model model) {

    addAdminLayout(model, "members");
    String trimmed = keyword == null ? "" : keyword.trim();
    PageResult<Members> memberPage = memberService.getMembersPaged(trimmed, page);
    if (memberPage.getTotalPages() > 0 && page > memberPage.getTotalPages()) {
      memberPage = memberService.getMembersPaged(trimmed, memberPage.getTotalPages());
    }
    model.addAttribute("totalCount", memberService.getTotalCount());
    model.addAttribute("members", memberPage.getContent());
    model.addAttribute("pageResult", memberPage);
    model.addAttribute("listCount", memberPage.getTotalItems());
    model.addAttribute("keyword", trimmed);

    return "admin/member-list";
  }

  /**
   * [5] 회원 상태 수정 처리
   * - String state를 사용하여 Enum 없이 직접 상태 문자열을 처리합니다.
   */
  @PostMapping("/members/{id}/update-status")
  public String updateMemberStatus(
      @PathVariable @Positive Long id,
      @RequestParam String state,
      @RequestParam(required = false, defaultValue = "") String keyword,
      @RequestParam(defaultValue = "1") int page) {

    memberService.changeMemberState(id, state);
    return redirectWithKeyword("/admin/members", keyword, page);
  }

  /**
   * [6] 회원 삭제 처리
   */
  @PostMapping("/members/{id}/delete")
  public String deleteMember(
      @PathVariable @Positive Long id,
      @RequestParam(required = false, defaultValue = "") String keyword,
      @RequestParam(defaultValue = "1") int page) {
    memberService.removeMember(id);
    return redirectWithKeyword("/admin/members", keyword, page);
  }
}