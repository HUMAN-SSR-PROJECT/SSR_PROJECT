package com.ssrpro.library.controller;

import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.*;
import com.ssrpro.library.dto.security.CustomUser;
import com.ssrpro.library.service.MemberService;
import com.ssrpro.library.support.BirthDateUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.Optional;

@Controller
@RequestMapping("/member")
@RequiredArgsConstructor
public class MemberController {
    private final MemberService memberService;

    @PostMapping("/join")
    public String join(SignUpReq req, RedirectAttributes redirectAttributes) {
        if (req.getBirth() == null) {
            redirectAttributes.addFlashAttribute("error", "생년월일 형식이 올바르지 않습니다. (예: 19970701)");
            return "redirect:/member/join";
        }
        if (!memberService.join(req)) {
            redirectAttributes.addFlashAttribute("error", "회원가입에 실패했습니다. 입력 정보를 확인해 주세요.");
            return "redirect:/member/join";
        }
        return "redirect:/member/login";
    }

    @GetMapping("/join")
    public String joinForm(@AuthenticationPrincipal CustomUser customUser) {
        if (customUser != null) {
            return "redirect:/";
        }
        return "member/join";
    }

    @PostMapping("/check-email")
    public String checkEmail(@RequestParam String email, RedirectAttributes redirectAttributes) {
        String trimmed = email == null ? "" : email.trim();
        redirectAttributes.addFlashAttribute("emailCheckValue", trimmed);
        if (trimmed.isEmpty()) {
            redirectAttributes.addFlashAttribute("emailCheck", "invalid");
            return "redirect:/member/join";
        }
        boolean exists = memberService.existsByEmail(trimmed);
        redirectAttributes.addFlashAttribute("emailCheck", exists ? "duplicate" : "available");
        return "redirect:/member/join";
    }

    @GetMapping("/login")
    public String loginForm(@AuthenticationPrincipal CustomUser customUser) {
        if (customUser != null) {
            return "redirect:/";
        }
        return "member/login";
    }

    @PostMapping("/find-id")
    public String findId(
            @RequestParam String name,
            @RequestParam String birth,
            RedirectAttributes redirectAttributes) {
        LocalDate birthDate = BirthDateUtils.parseYyyyMmDd(birth);
        if (name == null || name.isBlank() || birthDate == null) {
            redirectAttributes.addFlashAttribute("findIdStatus", "fail");
            return "redirect:/member/find-account?tab=id";
        }

        FindIdReq req = FindIdReq.builder().name(name.trim()).birth(birthDate).build();
        Optional<String> email = memberService.findId(req);
        if (email.isPresent()) {
            redirectAttributes.addFlashAttribute("findIdStatus", "success");
            redirectAttributes.addFlashAttribute("maskedEmail", BirthDateUtils.maskEmailLocal(email.get()));
        } else {
            redirectAttributes.addFlashAttribute("findIdStatus", "fail");
        }
        return "redirect:/member/find-account?tab=id";
    }

    @GetMapping("/find-account")
    public String findAccountForm(@RequestParam(defaultValue = "id") String tab, Model model) {
        model.addAttribute("tab", "pw".equals(tab) ? "pw" : "id");
        return "member/findAccount";
    }

    @GetMapping("/find-id")
    public String findIdForm() {
        return "redirect:/member/find-account?tab=id";
    }

    @GetMapping("/find-pw")
    public String findPwForm() {
        return "redirect:/member/find-account?tab=pw";
    }

    @GetMapping("/find_id")
    public String findIdFormLegacy() {
        return "redirect:/member/find-account?tab=id";
    }

    @PostMapping("/find-pw")
    public String findPw(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String birth,
            RedirectAttributes redirectAttributes) {
        LocalDate birthDate = BirthDateUtils.parseYyyyMmDd(birth);
        if (name == null || name.isBlank()
                || email == null || email.isBlank()
                || birthDate == null) {
            redirectAttributes.addFlashAttribute("findPwStatus", "fail");
            return "redirect:/member/find-account?tab=pw";
        }

        FindPwReq req = FindPwReq.builder()
                .name(name.trim())
                .email(email.trim())
                .birth(birthDate)
                .build();

        if (memberService.findPw(req)) {
            redirectAttributes.addFlashAttribute("findPwStatus", "success");
            redirectAttributes.addFlashAttribute("findPwEmail", req.getEmail());
        } else {
            redirectAttributes.addFlashAttribute("findPwStatus", "fail");
        }
        return "redirect:/member/find-account?tab=pw";
    }

    @PostMapping("/update")
    public String updateProfile(MypageUpdateReq req, @AuthenticationPrincipal CustomUser customUser) {
        if (customUser == null) {
            return "redirect:/member/login";
        }

        memberService.updateProfile(customUser.getMemberId(), req);
        return "redirect:/member/mypage";
    }

    @GetMapping("/mypage")
    public String mypageForm(@AuthenticationPrincipal CustomUser customUser, Model model) {
        if (customUser == null) {
            return "redirect:/member/login";
        }

        Long memberId = customUser.getMemberId();

        try {
            Members member = memberService.getMemberById(memberId);
            model.addAttribute("member", member);
            return "member/mypage";
        } catch (Exception e) {
            model.addAttribute("error", "조회중 에러가 발생했습니다");
            return "redirect:/";
        }
    }
}
