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

  @GetMapping("/readbook")
  public String readBook(Model model, @RequestParam(required = false) Long bookId) {
    // 내 서재 - 읽을 책 조회
    model.addAttribute("readSoonList", readBookService.readSoonList(memberId));

    // 내 서재 - 읽는 중 조회
    model.addAttribute("readingList", readBookService.readingList(memberId));
    
    // 내 서재 - 완독 조회
     model.addAttribute("readedList", readBookService.readedList(memberId));

    // 내 서재 - 완독 상세 조회
    if (bookId != null) {
      model.addAttribute("readedInfo", readBookService.readedInfo(memberId, bookId));
    }

    return "mylib/readbook";
  }

  // 읽을 책 추가 / 삭제
  @PostMapping("/readbook/{bookId}/soon")
  public String addToReadSoon(@PathVariable Long bookId) {
    readBookService.addToReadSoon(memberId, bookId);
    return "redirect:/mylib/readbook";
  }

  // 내 서재 - 읽는 중 추가 / 삭제
  @PostMapping("/readbook/{bookId}/reading")
  public String addToReading(@PathVariable Long bookId) {
    readBookService.addToReading(memberId, bookId);
    return "redirect:/mylib/readbook";
  }
  
  // 내 서재 - 읽는 중 → 완독, 완독 상세 수정
  @PostMapping("/readbook/{bookId}/readed")
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
