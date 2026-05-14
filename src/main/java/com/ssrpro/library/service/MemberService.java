package com.ssrpro.library.service;

import com.ssrpro.library.dao.MemberDao;
import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.FindIdReq;
import com.ssrpro.library.dto.request.FindPwReq;
import com.ssrpro.library.dto.request.LoginReq;
import com.ssrpro.library.dto.request.SignUpReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberDao memberDao;

    // 회원가입
    public boolean join(SignUpReq req) {
        // 1. 이메일 중복 체크 (null 및 trim 처리 포함)
        if (req.getEmail() == null || memberDao.existsByEmail(req.getEmail().trim())) {
            return false;
        }

        // 2. 비밀번호 유효성 체크
        if (req.getPassword() == null || req.getPassword().contains(" ")) {
            return false;
        }

        // 3. 입력값 정제 (이메일은 위에서 null 체크 완료)
        req.setEmail(req.getEmail().trim());
        if (req.getName() != null) req.setName(req.getName().trim());
        if (req.getNickname() != null) req.setNickname(req.getNickname().trim());

        // 4. 저장 실행
        Members member = req.toEntity();
        int result = memberDao.join(member);

        return result > 0;
    }

    // 로그인
    public boolean login(LoginReq req) {
        if (req.getEmail() == null || req.getPassword() == null) {
            return false;
        }
        req.setEmail(req.getEmail().trim());
        return memberDao.login(req).isPresent();
    }

    // 아이디 찾기
    public Optional<String> findId(FindIdReq req) {
        // 이름이나 생년월일 정보가 없으면 빈 Optional 반환
        if (req.getName() == null || req.getBirth() == null) {
            return Optional.empty();
        }
        req.setName(req.getName().trim());
        return memberDao.findId(req);
    }

    // 비밀번호 찾기
    public boolean findPw(FindPwReq req) {
        // 필수 정보(이름, 이메일, 생년월일) 누락 여부 확인
        if (req.getName() == null || req.getEmail() == null || req.getBirth() == null) {
            return false;
        }
        req.setName(req.getName().trim());
        req.setEmail(req.getEmail().trim());

        return memberDao.findPw(req);
    }
}