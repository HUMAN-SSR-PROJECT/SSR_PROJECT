package com.ssrpro.library.dto.security;

import com.ssrpro.library.dto.entity.Members;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

/**
 * CustomUser: Spring Security 인증 세션에 저장될 확장 유저 DTO
 * - 작성자: 성민 (with 야몽이)
 * - 역할: DB의 회원 정보를 시큐리티 인증 객체로 변환하고, 화면 출력용 데이터를 세션에 유지함
 */
@Getter
public class CustomUser extends User {

  // [성민님의 핵심 설계] 로그인 유지 기간 동안 화면에 보여줄 정보
  private final String email;
  private final String nickname;

  /**
   * [야몽이님의 제안 적용]
   * Member 객체를 통째로 받아서 생성자 내부에서 한 번에 처리합니다.
   */
  public CustomUser(Members member) {

    // 1. 시큐리티 부모(User)에게 필수 정보(아이디, 비번, 권한)를 넘겨줍니다.
    // Members 엔티티의 실제 필드명(email, password, rule)에 맞게 Getter를 호출합니다.
    super(
        member.getEmail(), // 시큐리티의 username 역할 (이메일)
        member.getPassword(), // 시큐리티의 password 역할

        // 삼항 연산자를 이용해 rule 값이 'Y'면 ADMIN, 아니면 USER 권한을 즉시 부여합니다.
        "Y".equals(member.getRule()) ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            : List.of(new SimpleGrantedAuthority("ROLE_USER")));

    // 2. 우리 프로젝트 화면(Thymeleaf 등)에서 꺼내 쓸 추가 정보를 세션에 저장합니다.
    this.email = member.getEmail();
    this.nickname = member.getNickname();
  }
}