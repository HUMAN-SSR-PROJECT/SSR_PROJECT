package com.ssrpro.library.dto.security;

import com.ssrpro.library.dto.entity.Members;
import lombok.Getter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.List;

@Getter
public class CustomUser extends User {

  private final Long memberId;
  private final String email;
  private final String nickname;

  public CustomUser(Members member) {
    // 1. 부모(User) 생성자에게 데이터 전달
    super(
            member.getEmail(),
            member.getPassword(),
            // Y/N 값을 시큐리티 권한 객체로 변환하여 전달
            List.of(new SimpleGrantedAuthority(convertRole(member.getRule())))
    );

    // 2. 추가 정보 저장
    this.memberId = member.getId();
    this.email = member.getEmail();
    this.nickname = member.getNickname();
  }

  // db의 role값을 변환 메서드
  private static String convertRole(String memberRole) {
    if ("Y".equals(memberRole)) {
      return "ROLE_ADMIN";
    }
    return "ROLE_USER"; // 'Y'가 아니면 모두 일반 유저로 처리
  }
}