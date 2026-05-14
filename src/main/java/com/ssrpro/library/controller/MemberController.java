package com.ssrpro.library.controller;

import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.*;
import com.ssrpro.library.service.MemberService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    // 회원가입 처리
    @PostMapping("/join")
    @ResponseBody
    public String join(SignUpReq req) {
        if (memberService.join(req)) {
            return "회원가입 성공! 로그인 페이지로 이동하세요.";
        }
        return "회원가입 실패: 중복된 이메일이거나 입력 형식이 잘못되었습니다.";
    }

    // 로그인 처리
    @PostMapping("/login")
    @ResponseBody // JSON이나 단순 문자열 응답이 필요한 비동기(AJAX) 로그인일 때 유지
    public String login(LoginReq req, HttpSession session) {
        Optional<Members> loginUserOpt = memberService.login(req);

        if (loginUserOpt.isPresent()) {
            Members loginUser = loginUserOpt.get();
            session.setAttribute("loginUser", loginUser);
            session.setAttribute("loginId", loginUser.getId());

            // 프런트엔드에서 이 응답을 받고 location.href = '/' 로 이동시키거나,
            // @ResponseBody를 제거하고 "redirect:/"를 반환하도록 설계할 수 있습니다.
            return "success";
        }

        return "fail";
    }
    // 아이디 찾기
    @PostMapping("/find-id")
    @ResponseBody
    public String findId(FindIdReq req) {
        Optional<String> email = memberService.findId(req);
        return email.map(s -> "찾으시는 이메일은: " + s)
                .orElse("해당 정보로 가입된 아이디가 없습니다.");
    }

    // 비밀번호 찾기
    @PostMapping("/find-pw")
    @ResponseBody
    public String findPw(FindPwReq req) {
        Optional<String> tempPassword = memberService.issueTempPassword(req);
        return tempPassword
                .map(pw -> "임시 비밀번호가 발급되었습니다: [" + pw + "] 로그인 후 반드시 비밀번호를 변경해주세요.")
                .orElse("입력하신 정보가 일치하지 않아 비밀번호를 발급할 수 없습니다.");
    }

    // 마이페이지 수정
    @PostMapping("/update")
    @ResponseBody
    public String updateProfile(MypageUpdateReq req, HttpSession session) {
        Long loginId = (Long) session.getAttribute("loginId");
        if (loginId == null) return "로그인이 필요한 서비스입니다.";

        if (memberService.updateProfile(loginId, req)) {
            // [중요] DB가 수정되었으므로, 세션에 저장된 loginUser 정보도 최신화합니다.
            // 이메일은 변하지 않는다고 가정하고 기존 이메일로 다시 조회합니다.
            Members currentMember = (Members) session.getAttribute("loginUser");
            memberService.getMemberByEmail(currentMember.getEmail())
                    .ifPresent(updated -> session.setAttribute("loginUser", updated));

            return "회원 정보가 성공적으로 수정되었습니다.";
        }
        return "정보 수정에 실패했습니다. 다시 시도해 주세요.";
    }

    // 로그아웃
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        // 세션의 모든 정복 삭제
        session.invalidate();
        return "redirect:/";
    }

    // 마이페이지 화면 이동
    @GetMapping("/mypage")
    public String mypageForm(HttpSession session) {
        // 로그인 체크
        if (session.getAttribute("loginUser") == null) {
            return "redirect:/member/login";
        }
        // 마이페이지 HTML 파일 이름 (mypage.html) 반환
        return "member/mypage";
    }
    // 내 서재로 이동
    @GetMapping("/library")
    public String myLibrary(HttpSession session) {
        if (session.getAttribute("loginUser") == null) {
            return "redirect:/member/login";
        }
        return "member/library";
    }
    // 독서통계로 이동
    @GetMapping("/statistics")
    public String statistics(HttpSession session) {
        if (session.getAttribute("loginUser") == null) return "redirect:/member/login";
        return "member/statistics";
    }
    @GetMapping("/admin")
    public String adminPage(HttpSession session) {
        // 1. 세션에서 Members 객체를 꺼냅니다. (이메일 String이 아님에 주의!)
        Members loginUser = (Members) session.getAttribute("loginUser");

        // 2. 로그인 여부 및 관리자 권한을 서비스 로직으로 체크합니다.
        if (loginUser == null) {
            return "redirect:/member/login";
        }

        if (!memberService.isAdmin(loginUser)) {
            // 관리자가 아니면 메인으로 쫓아냅니다.
            return "redirect:/";
        }

        return "admin/main";
    }
}
