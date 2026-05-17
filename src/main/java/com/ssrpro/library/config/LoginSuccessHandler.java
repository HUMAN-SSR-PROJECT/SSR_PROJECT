package com.ssrpro.library.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.RequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.web.servlet.FlashMap;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.io.IOException;

/**
 * 로그인 성공 시 flash 알림 후, 이전에 접근하려던 URL(있으면) 또는 메인으로 이동합니다.
 */
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final RequestCache requestCache = new HttpSessionRequestCache();

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        FlashMap flashMap = RequestContextUtils.getOutputFlashMap(request);
        if (flashMap != null) {
            flashMap.put("success", "로그인되었습니다.");
        }

        SavedRequest savedRequest = requestCache.getRequest(request, response);
        String redirectUrl = request.getContextPath() + "/";
        if (savedRequest != null && "GET".equalsIgnoreCase(savedRequest.getMethod())) {
            redirectUrl = savedRequest.getRedirectUrl();
        }
        response.sendRedirect(redirectUrl);
    }
}
