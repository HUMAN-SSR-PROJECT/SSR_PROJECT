package com.ssrpro.library.controller;

import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.*;
import com.ssrpro.library.dto.security.CustomUser;
import com.ssrpro.library.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    // 회원가입 처리
    @PostMapping("/join")
    @ResponseBody
    public String join(SignUpReq req, Model model) {
        if (!memberService.join(req)) {
            model.addAttribute("error", "회원가입에 실패했습니다");
            return "redirect:member/join";
        }else{
            return "redirect:member/login";
        }
    }
    // 회원가입 이동
    @GetMapping("/join")
    public String joinForm(HttpSession session) {
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/";
        }
        return "member/join";
    }

    // 로그인 이동
    @GetMapping("/login")
    public String loginForm(HttpSession session) {
        // 이미 로그인 상태면 메인페이지로 이동
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/";
        }
        return "member/login";
    }
    // 아이디 찾기
    @PostMapping("/find-id")
    @ResponseBody
    public String findId(FindIdReq req) {
        Optional<String> email = memberService.findId(req);
        return email.map(s -> "찾으시는 이메일은: " + s)
                .orElse("해당 정보로 가입된 아이디가 없습니다.");
    }
    // 아이디 찾기 이동
    @GetMapping("/find_id")
    public String findIdForm() {
        return "member/findId";
    }

    // 비밀번호 찾기
    @PostMapping("/find-pw")
    @ResponseBody
    public String findPw(FindPwReq req) {
        // 단순 true/false 결과만 확인
        if (memberService.findPw(req)) {
            return "true";  // 혹은 "인증 성공" 등의 메시지
        } else {
            return "false"; // 혹은 "인증 실패" 등의 메시지
        }
    }
    // 비밀번호 찾기 이동
    @GetMapping("/find-pw")
    public String findPwForm() {
        return "member/findPw";
    }

    // 마이페이지 수정
    @PostMapping("/update")
    @ResponseBody
    public String updateProfile(MypageUpdateReq req, @AuthenticationPrincipal CustomUser customUser) {
        // 1. 로그인 체크 (혹시 모르니)
        if (customUser == null) {
            return "redirect:/member/login";
        }

        // 2. ID 꺼내기
        Long memberId = customUser.getMemberId();

        memberService.updateProfile(memberId, req);
        return "redirect:member/mypage";
    }

    // 마이페이지 화면 이동
    @GetMapping("/mypage")
    public String mypageForm(@AuthenticationPrincipal CustomUser customUser, Model model) {
        // 1. 로그인 체크 (혹시 모르니)
        if (customUser == null) {
            return "redirect:member/login";
        }

        // 2. ID 꺼내기
        Long memberId = customUser.getMemberId();

        // 2. DB에서 최신 회원 정보를 조회합니다.
        try{
            Members member = memberService.getMemberById(memberId);

            model.addAttribute("member", member);
            return "member/mypage";
        }catch(Exception e){
            model.addAttribute("error", "조회중 에러가 발생했습니다");
            return "redirect:/";
        }
    }
}
