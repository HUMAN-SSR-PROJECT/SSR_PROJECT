package com.ssrpro.library.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.ssrpro.library.dao.MemberDao;
import com.ssrpro.library.dto.entity.Members;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SecurityService implements UserDetailsService{
  private final MemberDao memberDao;
  
  @Override
  public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
    Members member = memberDao.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 이메일입니다."));
    return new CustomUser(member);
  }
}
