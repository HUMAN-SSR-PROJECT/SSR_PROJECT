package com.ssrpro.library.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity // 시큐리티 설정을 활성화 함
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 권한별 경로 설정 (인가)
                .authorizeHttpRequests(auth -> auth
                        // /admin으로 시작하면 관리자만
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // 마이페이지는 로그인만 하면 ok
                        .requestMatchers("/member/mypage").authenticated()
                        // 그 외(메인, 도서검색 등)는 비로그인도 가능
                        .anyRequest().permitAll()
                )
                // 2. 로그인 설정
                .formLogin(form -> form
                        // 커스텀 로그인 페이지 경로
                        .loginPage("/member/login")
                        // 로그인 성공시 이동할 페이지
                        .defaultSuccessUrl("/", true)
                )
                // 3. 로그아웃 설정
                .logout(logout -> logout
                        .logoutUrl("/member/logout") // 로그아웃을 처리할 URL만 지정
                        .logoutSuccessUrl("/")       // 성공 시 이동할 페이지
                        .invalidateHttpSession(true) // 세션 무효화
                        .deleteCookies("JSESSIONID") // (선택사항) 쿠키 삭제까지 추가하면 더 안전합니다.
                );
        return http.build();
    }
    // 비밀번호 암호화 빈 등록 (회원가입 시 사용)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
