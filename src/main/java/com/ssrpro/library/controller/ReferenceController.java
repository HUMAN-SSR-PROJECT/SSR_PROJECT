package com.ssrpro.library.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * 빈 상태 UI 참고용 미리보기 (Figma 1:3166). 운영 화면 라우트가 아닙니다.
 */
@Controller
@RequestMapping("/reference/empty-states")
public class ReferenceController {

    @GetMapping("/catalog")
    public String catalog() {
        return "reference/empty-states-catalog";
    }

    @GetMapping("/{slug}")
    public String preview(@PathVariable String slug) {
        return "reference/empty-states/" + slug;
    }
}
