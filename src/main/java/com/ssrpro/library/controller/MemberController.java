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
    public String join(SignUpReq req) {
        if (memberService.join(req)) {
            return "회원가입 성공! 로그인 페이지로 이동하세요.";
        }
        return "회원가입 실패: 중복된 이메일이거나 입력 형식이 잘못되었습니다.";
    }
    // 회원가입 이동
    @GetMapping("/join")
    public String joinForm(HttpSession session) {
        if (session.getAttribute("loginUser") != null) {
            return "redirect:/";
        }
        return "member/join";
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
        if (memberService.checkMemberExists(req)) {
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
    public String updateProfile(MypageUpdateReq req, HttpSession session) {
        // 1. 세션에서 로그인 ID 확인
        Long loginId = (Long) session.getAttribute("loginId");
        if (loginId == null) {
            return "로그인이 필요한 서비스입니다.";
        }

        // 2. DB 업데이트 실행
        if (memberService.updateProfile(loginId, req)) {

            // 3. 세션 최신화 (가장 안전한 방법)
            // 세션에서 꺼낸 객체가 Members 타입인지 확인 후 진행
            Object userObj = session.getAttribute("loginUser");
            if (userObj instanceof Members) {
                Members currentMember = (Members) userObj;

                // 서비스에서 최신 정보를 다시 가져와 세션에 덮어쓰기
                memberService.getMemberByEmail(currentMember.getEmail())
                        .ifPresent(updated -> session.setAttribute("loginUser", updated));
            }

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
    public String mypageForm(HttpSession session, org.springframework.ui.Model model) {
        // 1. 세션에서 로그인된 ID를 꺼냅니다.
        Long loginId = (Long) session.getAttribute("loginId");

        if (loginId == null) {
            return "redirect:/member/login";
        }

        // 2. DB에서 최신 회원 정보를 조회합니다.
        Optional<Members> memberOpt = memberService.getMemberById(loginId);

        if (memberOpt.isPresent()) {
            // 3. 모델에 담아서 HTML로 보냅니다. (화면에서 ${member.nickname} 등으로 사용)
            model.addAttribute("member", memberOpt.get());
            return "member/mypage";
        }

        return "redirect:/"; // 유저 정보가 없으면 메인으로
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
    public String adminPage(HttpSession session, String keyword, org.springframework.ui.Model model) {
        // 1. 관리자 권한 체크
        Members loginUser = (Members) session.getAttribute("loginUser");
        if (loginUser == null || !memberService.isAdmin(loginUser)) {
            return "redirect:/";
        }

        // 2. 회원 목록 가져오기 (keyword가 없으면 전체, 있으면 검색)
        List<Members> memberList = memberService.getAllMembers(keyword);
        model.addAttribute("memberList", memberList);
        model.addAttribute("keyword", keyword); // 검색어 유지용

        return "admin/main";
    }
    // 상태 변경 API (AJAX 호출용)
    @PostMapping("/admin/update-state")
    @ResponseBody
    public String updateState(Long id, String state) {
        if (memberService.changeMemberState(id, state)) {
            return "success";
        }
        return "fail";
    }

    // 회원 삭제 API (AJAX 호출용)
    @PostMapping("/admin/delete")
    @ResponseBody
    public String deleteMember(Long id) {
        if (memberService.removeMember(id)) {
            return "success";
        }
        return "fail";
    }
}
