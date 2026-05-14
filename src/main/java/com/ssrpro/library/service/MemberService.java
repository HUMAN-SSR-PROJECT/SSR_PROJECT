package com.ssrpro.library.service;

import com.ssrpro.library.dao.MemberDao;
import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.*;
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
    public Optional<Members> login(LoginReq req) {
        if (req.getEmail() == null || req.getPassword() == null) {
            return Optional.empty();
        }
        req.setEmail(req.getEmail().trim());
        // Dao에서 이미 이메일과 비밀번호가 맞는 유저를 찾아오므로,
        // 여기서 바로 객체를 반환하면 컨트롤러에서 findByEmail을 다시 부를 필요가 없습니다.
        return memberDao.login(req);
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

    // 임시 비밀번호 발송 구역
    public Optional<String> issueTempPassword(FindPwReq req) {
        if (this.findPw(req)) {
            String tempPw = java.util.UUID.randomUUID().toString().substring(0, 8);
            int result = memberDao.updatePassword(req.getEmail().trim(), tempPw);
            return result > 0 ? Optional.of(tempPw) : Optional.empty();
        }
        return Optional.empty();
    }

    // 마이페이지
    public boolean updateProfile(Long memberId, MypageUpdateReq req) {
        // 1. DTO를 엔티티로 변환 (image_bc7ccb.png의 toEntity 활용)
        Members member = req.toEntity(memberId);

        // 2. DB 업데이트 실행
        int result = memberDao.updateMemberProfile(member);

        return result > 0;
    }
    // 관리자 판별
    public boolean isAdmin(Members member) {
        // null 체크와 대소문자 구분 없는 비교로 안전성 강화
        return member != null && "ADMIN".equalsIgnoreCase(member.getRule());
    }
    // 정보 가져오기 로직
    public Optional<Members> getMemberByEmail(String email) {
        if (email == null || email.isBlank()) return Optional.empty();
        return memberDao.findByEmail(email.trim());
    }
}