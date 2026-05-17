package com.ssrpro.library.service;

import com.google.cloud.storage.Bucket;
import com.ssrpro.library.dao.MemberDao;
import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberDao memberDao;
    private final PasswordEncoder passwordEncoder;
    private final Bucket storageBucket;

    @Value("${firebase.storage.bucket}")
    private String bucketName;

    public boolean existsByEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return memberDao.existsByEmail(email.trim());
    }

    public boolean existsByNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }
        return memberDao.existsByNickname(nickname.trim());
    }

    /**
     * 회원가입. 성공 시 empty, 실패 시 사용자에게 보여줄 메시지.
     */
    public Optional<String> join(SignUpReq req) {
        if (req.getEmail() == null || req.getEmail().isBlank()) {
            return Optional.of("이메일을 입력해 주세요.");
        }
        if (req.getNickname() == null || req.getNickname().isBlank()) {
            return Optional.of("닉네임을 입력해 주세요.");
        }

        String email = req.getEmail().trim();
        String nickname = req.getNickname().trim();

        if (memberDao.existsByEmail(email)) {
            return Optional.of("이미 사용 중인 이메일입니다.");
        }
        if (memberDao.existsByNickname(nickname)) {
            return Optional.of("이미 사용 중인 닉네임입니다.");
        }

        if (req.getPassword() == null || req.getPassword().contains(" ")) {
            return Optional.of("비밀번호를 확인해 주세요. (공백 불가)");
        }

        req.setPassword(passwordEncoder.encode(req.getPassword()));
        req.setEmail(email);
        if (req.getName() != null) {
            req.setName(req.getName().trim());
        }
        req.setNickname(nickname);

        Members member = req.toEntity();
        int result = memberDao.join(member);
        if (result <= 0) {
            return Optional.of("회원가입에 실패했습니다. 잠시 후 다시 시도해 주세요.");
        }
        return Optional.empty();
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
    // 마이페이지 수정
    public boolean updateProfile(Long memberId, MypageUpdateReq req) {
        String imageUrl = null;

        // 1. 이미지가 첨부되었는지 확인
        if (req.getImgUrl() != null && !req.getImgUrl().isEmpty()) {
            MultipartFile file = req.getImgUrl();

            // 파일명 생성 (중복 방지를 위해 memberId와 타임스탬프 조합)
            String fileName = "profile/" + memberId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

            try {
                // 2. Firebase Storage에 파일 업로드
                storageBucket.create(fileName, file.getBytes(), file.getContentType());
            } catch (IOException e) {
                throw new IllegalStateException("프로필 이미지 업로드에 실패했습니다.", e);
            }

            // 3. 업로드된 파일의 공용 URL 생성 (고정 형식)
            imageUrl = String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    bucketName, fileName.replace("/", "%2F"));
        }

        // 4. 엔티티 변환 (imageUrl 포함)
        Members member = req.toEntity(memberId);
        if (imageUrl != null) {
            member.setImgUrl(imageUrl); // 엔티티에 사진 URL 세팅
        }

        // 5. DB 업데이트
        int result = memberDao.updateMemberProfile(member);
        return result > 0;
    }

    // 마이페이지 조회를 위한 회원 정보 획득
    public Members getMemberById(Long id) {
        return memberDao.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원 입니다."));
    }

   // 관리자용 회원 목록 조회
    public List<Members> getAllMembers(String keyword) {
        // 키워드가 null이면 빈 문자열로 처리하여 전체 조회가 되도록 함
        return memberDao.findAll(keyword == null ? "" : keyword);
    }

    // 관리자 회원 상태 변경
    public boolean changeMemberState(Long id, String state) {
        return memberDao.updateMemberState(id, state) > 0;
    }

    // 관리자 회원 삭제
    public boolean removeMember(Long id) {
        return memberDao.deleteMember(id) > 0;
    }

    // 대시보드용 총 회원 수 획득
    public int getTotalCount() {
        return memberDao.getTotalMemberCount();
    }

    // 대시보드용 최근 가입 회원 10명 목록 획득
    public List<Members> getRecentMembers() {
        return memberDao.findRecentMembers();
    }
}