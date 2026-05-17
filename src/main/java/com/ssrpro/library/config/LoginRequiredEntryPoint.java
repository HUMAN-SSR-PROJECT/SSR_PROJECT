package com.ssrpro.library.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.io.IOException;

/**
 * 비로그인 사용자가 인증이 필요한 URL에 접근할 때 로그인 페이지로 보냅니다.
 */
public class LoginRequiredEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.sendRedirect(request.getContextPath() + "/member/login?needLogin=true");
    }
}
