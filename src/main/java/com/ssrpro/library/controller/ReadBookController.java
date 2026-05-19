package com.ssrpro.library.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ssrpro.library.dto.request.ReadBookReq;
import com.ssrpro.library.dto.security.CustomUser;
import com.ssrpro.library.service.ReadBookService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mylib")
@RequiredArgsConstructor
public class ReadBookController {
  private final ReadBookService readBookService;

  @GetMapping("/readbook")
  public String readBook(
    Model model,
    @AuthenticationPrincipal CustomUser customUser,
    @RequestParam(required = false, defaultValue = "reading") String tab
  ) {
    Long memberId = customUser.getMemberId();

    var readSoonList = readBookService.readSoonList(memberId);
    readSoonList.forEach(book ->
        book.setReadingState(readBookService.readingState(memberId, book.getBookId())));
    var readingList = readBookService.readingList(memberId);
    var readedList = readBookService.readedList(memberId);

    model.addAttribute("readSoonList", readSoonList);
    model.addAttribute("readingList", readingList);
    model.addAttribute("readedList", readedList);

    int soonCount = readSoonList.size();
    int readingCount = readingList.size();
    int finishedCount = readedList.size();
    model.addAttribute("soonCount", soonCount);
    model.addAttribute("readingCount", readingCount);
    model.addAttribute("finishedCount", finishedCount);
    model.addAttribute("totalCount", soonCount + readingCount + finishedCount);

    String activeTab = tab;
    if (!"reading".equals(activeTab) && !"soon".equals(activeTab) && !"finished".equals(activeTab)) {
      activeTab = "reading";
    }
    model.addAttribute("activeTab", activeTab);
    model.addAttribute("mainClass", "site-main--fluid");

    return "mylib/readbook";
  }

  // 읽을 책 추가 / 삭제
  @PostMapping("/readbook/{bookId}/soon")
  public String addToReadSoon(
    @AuthenticationPrincipal CustomUser customUser, 
    @PathVariable Long bookId
  ) {
    Long memberId = customUser.getMemberId();

    readBookService.addToReadSoon(memberId, bookId);
    return "redirect:/mylib/readbook";
  }

  // 내 서재 - 읽는 중 추가 / 삭제 (토글)
  @PostMapping("/readbook/{bookId}/reading")
  public String addToReading(
    @AuthenticationPrincipal CustomUser customUser,
    @PathVariable Long bookId
  ) {
    Long memberId = customUser.getMemberId();

    readBookService.addToReading(memberId, bookId);
    return "redirect:/mylib/readbook";
  }

  /** 읽을 책 탭 — 읽는 중 추가만 (read_soon 유지) */
  @PostMapping("/readbook/{bookId}/reading-from-soon")
  public String addToReadingFromWishlist(
      @AuthenticationPrincipal CustomUser customUser,
      @PathVariable Long bookId,
      RedirectAttributes redirectAttributes) {
    Long memberId = customUser.getMemberId();

    try {
      int before = readBookService.readingState(memberId, bookId);
      readBookService.addToReadingFromWishlist(memberId, bookId);
      String message = before == 2
          ? "이미 읽는 중 목록에 있습니다. 읽을 책(즐겨찾기)은 그대로 유지됩니다."
          : "읽는 중에 추가했습니다. 읽을 책(즐겨찾기)은 그대로 유지됩니다.";
      redirectAttributes.addFlashAttribute("success", message);
    } catch (IllegalStateException e) {
      redirectAttributes.addFlashAttribute("readBookError", e.getMessage());
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "처리 중 오류가 발생했습니다.");
    }
    return "redirect:/mylib/readbook?tab=soon";
  }
  
  // 내 서재 - 읽는 중 → 완독, 완독 상세 수정
  @PostMapping("/readbook/readed")
  public String changeToReaded(
    @AuthenticationPrincipal CustomUser customUser,
    @RequestParam Long bookId,
    @RequestParam(required = false, defaultValue = "reading") String returnTab,
    @ModelAttribute ReadBookReq req,
    RedirectAttributes redirectAttributes
  ) {
    Long memberId = customUser.getMemberId();
    req.setBookId(bookId);
    String errorTab = resolveMylibTab(returnTab);

    try {
      readBookService.changeToReaded(memberId, req);
      redirectAttributes.addFlashAttribute("success", "완독 기록을 저장했습니다.");
      return "redirect:/mylib/readbook?tab=finished";
    } catch (IllegalStateException e) {
      redirectAttributes.addFlashAttribute("readBookError", e.getMessage());
      return "redirect:/mylib/readbook?tab=" + errorTab;
    } catch (Exception e) {
      redirectAttributes.addFlashAttribute("error", "완독 처리 중 오류가 발생했습니다.");
      return "redirect:/mylib/readbook?tab=" + errorTab;
    }
  }

  private static String resolveMylibTab(String tab) {
    if ("soon".equals(tab) || "finished".equals(tab) || "reading".equals(tab)) {
      return tab;
    }
    return "reading";
  }
  
  @GetMapping("/stats")
  public String readingStats(Model model, @AuthenticationPrincipal CustomUser customUser) {
    Long memberId = customUser.getMemberId();
    model.addAttribute("stats", readBookService.buildReadingStats(memberId));
    model.addAttribute("mainClass", "site-main--fluid");
    return "mylib/stats";
  }
}
