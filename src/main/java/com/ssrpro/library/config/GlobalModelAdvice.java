package com.ssrpro.library.config;

import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.response.HeaderProfileView;
import com.ssrpro.library.dto.security.CustomUser;
import com.ssrpro.library.service.MemberService;
import com.ssrpro.library.service.ReadBookService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalModelAdvice {

    private final ReadBookService readBookService;
    private final MemberService memberService;

    @Value("${kakao.map.key}")
    private String kakaoMapKey;

    @ModelAttribute("kakaoMapKey")
    public String kakaoMapKey() {
        return kakaoMapKey;
    }

    @ModelAttribute("headerProfile")
    public HeaderProfileView headerProfile(@AuthenticationPrincipal CustomUser user) {
        if (user == null) {
            return HeaderProfileView.guest();
        }
        try {
            Members member = memberService.getMemberById(user.getMemberId());
            String url = member.getImgUrl();
            String imgUrl = (url != null && !url.isBlank()) ? url : null;
            return new HeaderProfileView(imgUrl, MemberService.avatarInitial(member));
        } catch (Exception e) {
            return HeaderProfileView.guest();
        }
    }

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
