package com.ssrpro.library.config;

import com.ssrpro.library.dto.security.CustomUser;
import com.ssrpro.library.service.ReadBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final ReadBookService readBookService;

    @ModelAttribute("readingCount")
    public int readingCount(@AuthenticationPrincipal CustomUser user) {
        if (user == null) {
            return 0;
        }
        try {
            return readBookService.readingList(user.getMemberId()).size();
        } catch (Exception e) {
            return 0;
        }
    }

    @ModelAttribute("finishedCount")
    public int finishedCount(@AuthenticationPrincipal CustomUser user) {
        if (user == null) {
            return 0;
        }
        try {
            return readBookService.readedList(user.getMemberId()).size();
        } catch (Exception e) {
            return 0;
        }
    }
}
