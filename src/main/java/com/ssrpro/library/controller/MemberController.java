package com.ssrpro.library.controller;

import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.*;
import com.ssrpro.library.dto.security.CustomUser;
import com.ssrpro.library.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    // 회원가입 처리
    @PostMapping("/join")
    public String join(SignUpReq req, RedirectAttributes redirectAttributes) {
        if (!memberService.join(req)) {
            redirectAttributes.addFlashAttribute("error", "회원가입에 실패했습니다");
            return "redirect:/member/join";
        }
        return "redirect:/member/login";
    }
    // 회원가입 이동
    @GetMapping("/join")
    public String joinForm(@AuthenticationPrincipal CustomUser customUser) {
        if (customUser != null) {
            return "redirect:/";
        }
        return "member/join";
    }

    // 로그인 이동
    @GetMapping("/login")
    public String loginForm(@AuthenticationPrincipal CustomUser customUser) {
        if (customUser != null) {
            return "redirect:/";
        }
        return "member/login";
    }
    // 아이디 찾기
    @PostMapping("/find-id")
    public String findId(FindIdReq req, RedirectAttributes redirectAttributes) {
        if (req.getName() == null || req.getName().isBlank() || req.getBirth() == null) {
            redirectAttributes.addFlashAttribute("error", "이름과 생년월일을 입력해 주세요.");
            return "redirect:/member/find-id";
        }
        Optional<String> email = memberService.findId(req);
        if (email.isPresent()) {
            redirectAttributes.addFlashAttribute("message", "찾으시는 이메일은: " + email.get());
        } else {
            redirectAttributes.addFlashAttribute("error", "해당 정보로 가입된 아이디가 없습니다.");
        }
        return "redirect:/member/find-id";
    }
    // 아이디 찾기 이동
    @GetMapping("/find-id")
    public String findIdForm() {
        return "member/findId";
    }

    @GetMapping("/find_id")
    public String findIdFormLegacy() {
        return "redirect:/member/find-id";
    }

    // 비밀번호 찾기
    @PostMapping("/find-pw")
    public String findPw(FindPwReq req, RedirectAttributes redirectAttributes) {
        if (req.getName() == null || req.getName().isBlank()
                || req.getEmail() == null || req.getEmail().isBlank()
                || req.getBirth() == null) {
            redirectAttributes.addFlashAttribute("error", "이름, 이메일, 생년월일을 모두 입력해 주세요.");
            return "redirect:/member/find-pw";
        }
        if (memberService.findPw(req)) {
            redirectAttributes.addFlashAttribute("message", "입력하신 정보와 일치하는 회원이 있습니다.");
        } else {
            redirectAttributes.addFlashAttribute("error", "일치하는 회원 정보가 없습니다.");
        }
        return "redirect:/member/find-pw";
    }
    // 비밀번호 찾기 이동
    @GetMapping("/find-pw")
    public String findPwForm() {
        return "member/findPw";
    }

    // 마이페이지 수정
    @PostMapping("/update")
    public String updateProfile(MypageUpdateReq req, @AuthenticationPrincipal CustomUser customUser) {
        if (customUser == null) {
            return "redirect:/member/login";
        }

        memberService.updateProfile(customUser.getMemberId(), req);
        return "redirect:/member/mypage";
    }

    // 마이페이지 화면 이동
    @GetMapping("/mypage")
    public String mypageForm(@AuthenticationPrincipal CustomUser customUser, Model model) {
        // 1. 로그인 체크 (혹시 모르니)
        if (customUser == null) {
            return "redirect:/member/login";
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
