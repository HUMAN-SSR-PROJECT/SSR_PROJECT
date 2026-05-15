package com.ssrpro.library.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.ssrpro.library.service.ReadBookService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Controller
@RequestMapping("/mylib")
@RequiredArgsConstructor
public class ReadBookController {
  private final ReadBookService readBookService;

  @GetMapping("/readbook/{bookId}")
  public String readBook(Model model, @PathVariable Long bookId) {
    // 내 서재 - 읽을 책 조회
    try {
      model.addAttribute("readSoonList", readBookService.readSoonList(memberId));
    } catch (Exception e) {
      model.addAttribute("readSoonListError", e.getMessage());
    }

    // 내 서재 - 읽는 중 조회
    try {
      model.addAttribute("readingList", readBookService.readingList(memberId));
    } catch (Exception e) {
      model.addAttribute("readingListError", e.getMessage());
    }
    
    // 내 서재 - 완독 조회
    try {
      model.addAttribute("readedList", readBookService.readedList(memberId));
    } catch (Exception e) {
      model.addAttribute("readedListError", e.getMessage());
    }

    // 내 서재 - 완독 상세 조회
    try {
      model.addAttribute("readedInfo", readBookService.readedInfo(memberId, bookId));
    } catch (Exception e) {
      model.addAttribute("readedInfoError", e.getMessage());
    }
    
    return "mylib/readbook";
  }

  // 읽을 책 추가
  @PostMapping("/readsoon/{bookId}/add")
  public String addToReadSoon(@PathVariable Long bookId) {
    readBookService.addToReadSoon(memberId, bookId);
    return "redirect:/mylib/readbook";
  }

  // 내 서재 - 읽을 책 삭제
  @PostMapping("/readsoon/{bookId}/delete")
  public String deleteReadSoon(@PathVariable Long bookId) {
    readBookService.deleteReadSoon(memberId, bookId);
    return "redirect:/mylib/readbook";
  }

  // 내 서재 - 읽을 책 → 읽는 중
  @PostMapping("/readbook/{bookId}/add")
  public String addToReading(@PathVariable Long bookId) {
    readBookService.addToReading(memberId, bookId);
    return "redirect:/mylib/readbook";
  }
  
  // 내 서재 - 읽는 중 / 완독 삭제
  @PostMapping("/readbook/{bookId}/delete")
  public String deleteReading(@PathVariable Long bookId) {
    readBookService.deleteReading(memberId, bookId);
    return "redirect:/mylib/readbook";
  }
  
  // 내 서재 - 읽는 중 → 완독, 완독 상세 수정
  @PostMapping("/readbook/{bookId}/update")
  public String changeToReaded(@PathVariable Long bookId) {
    readBookService.changeToReaded(memberId, bookId);
    return "redirect:/mylib/readbook";
  }
  
  // 독서 분석 리포트 - 통계 계산용 리스트
  @GetMapping("/stats")
  public String rawDate(Model model) {
    model.addAttribute("rawDate", readBookService.getRawData(memberId));
    return "mylib/stats";
  }
  
}
