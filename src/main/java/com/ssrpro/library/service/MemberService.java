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
import com.ssrpro.library.dto.response.PageResult;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    // 마이페이지 수정 — 실패 시 메시지
    public Optional<String> updateProfile(Long memberId, MypageUpdateReq req) {
        if (req == null) {
            return Optional.of("요청 정보가 없습니다.");
        }
        String nickname = req.getNickname() == null ? "" : req.getNickname().trim();
        if (nickname.isBlank()) {
            return Optional.of("닉네임을 입력해 주세요.");
        }
        if (memberDao.existsByNicknameExcept(memberId, nickname)) {
            return Optional.of("이미 사용 중인 닉네임입니다.");
        }

        Members existing = getMemberById(memberId);
        String imageUrl = existing.getImgUrl();

        MultipartFile file = req.getImgUrl();
        if (file != null && !file.isEmpty()) {
            String fileName =
                "profile/"
                    + memberId
                    + "_"
                    + System.currentTimeMillis()
                    + "_"
                    + file.getOriginalFilename();

            try {
                storageBucket.create(fileName, file.getBytes(), file.getContentType());
            } catch (IOException e) {
                return Optional.of("프로필 이미지 업로드에 실패했습니다.");
            }

            imageUrl =
                String.format(
                    "https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                    bucketName,
                    fileName.replace("/", "%2F"));
        }

        Members member = req.toEntity(memberId);
        member.setNickname(nickname);
        member.setImgUrl(imageUrl);
        member.setIntro(trimToNull(req.getIntro()));
        member.setAddr(trimToNull(req.getAddr()));

        int result = memberDao.updateMemberProfile(member);
        return result > 0 ? Optional.empty() : Optional.of("프로필 저장에 실패했습니다.");
    }

    public boolean withdrawMember(Long memberId) {
        return memberDao.deleteMember(memberId) > 0;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** 아바타 이니셜 (이미지 없을 때) */
    public static String avatarInitial(Members member) {
        if (member == null) {
            return "?";
        }
        String source = member.getNickname();
        if (source == null || source.isBlank()) {
            source = member.getName();
        }
        if (source == null || source.isBlank()) {
            return "?";
        }
        return source.substring(0, 1).toUpperCase();
    }

    // 마이페이지 조회를 위한 회원 정보 획득
    public Members getMemberById(Long id) {
        return memberDao.findById(id).orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원 입니다."));
    }

    /** 관리자 회원 목록 — 페이지당 {@link PageResult#DEFAULT_SIZE}건 */
    public PageResult<Members> getMembersPaged(String keyword, int page) {
        int safePage = Math.max(page, 1);
        String trimmed = keyword == null ? "" : keyword.trim();
        int offset = (safePage - 1) * PageResult.DEFAULT_SIZE;
        long total = memberDao.countAll(trimmed);
        List<Members> content = memberDao.findAllPaged(trimmed, offset, PageResult.DEFAULT_SIZE);
        return PageResult.of(content, safePage, PageResult.DEFAULT_SIZE, total);
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