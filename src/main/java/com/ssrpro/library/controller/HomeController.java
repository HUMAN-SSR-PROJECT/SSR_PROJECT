package com.ssrpro.library.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("mainClass", "site-main--fluid");
        return "index";
    }

    /** Spring Security 기본 실패 URL(/login) 호환 — 로그인 페이지로 보냄 */
    @GetMapping("/login")
    public String loginLegacyRedirect(@RequestParam(required = false) String error) {
        return error != null ? "redirect:/member/login?error" : "redirect:/member/login";
    }
}
