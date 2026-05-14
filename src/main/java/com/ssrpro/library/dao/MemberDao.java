package com.ssrpro.library.dao;

import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.FindIdReq;
import com.ssrpro.library.dto.request.FindPwReq;
import com.ssrpro.library.dto.request.LoginReq;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MemberDao {
    private final JdbcTemplate jdbcTemplate;
    // 회원가입
    public int join(Members members) {
        String sql = "INSERT INTO MEMBERS (MEMBER_ID, MEMBER_EMAIL, MEMBER_PASSWORD, MEMBER_NAME, " +
                      "MEMBER_NICKNAME, MEMBER_BIRTH, MEMBER_STATE, MEMBER_RULE) " +
                      "VALUES (MEMBER_SEQ.NEXTVAL, ?, ?, ?, ?, ?, ?, ?)";
        return jdbcTemplate.update(sql,
                members.getEmail().trim(), members.getPassword(), members.getName().trim(),
                members.getNickname().trim(), members.getBirth(),
                members.getState(), members.getRule());
    }

    // 로그인
    public Optional<Members> login(LoginReq req) {
        String sql = "SELECT * FROM MEMBERS WHERE MEMBER_EMAIL = ? AND MEMBER_PASSWORD = ?";
        return jdbcTemplate.query(sql, memberRowMapper(), req.getEmail().trim(), req.getPassword())
                .stream().findFirst();
    }

    // 아이디 찾기
    public Optional<String> findId(FindIdReq req) {
        String sql = "SELECT MEMBER_EMAIL FROM MEMBERS WHERE MEMBER_NAME = ? AND MEMBER_BIRTH = ?";
        return jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("MEMBER_EMAIL"),
                req.getName().trim(), req.getBirth()).stream().findFirst();
    }

    // 비밀번호 찾기
    public boolean findPw(FindPwReq req) {
        String sql = "SELECT COUNT(*) FROM MEMBERS WHERE MEMBER_NAME = ? AND MEMBER_EMAIL = ? AND MEMBER_BIRTH = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                req.getName().trim(), req.getEmail().trim(), req.getBirth());
        return count != null && count > 0;
    }

    // DB 조회 결과를 Members 객체로 매핑하는 도우미 메서드
    private RowMapper<Members> memberRowMapper() {
        return (rs, rowNum) -> Members.builder()
                .id(rs.getLong("MEMBER_ID"))
                .email(rs.getString("MEMBER_EMAIL"))
                .password(rs.getString("MEMBER_PASSWORD"))
                .name(rs.getString("MEMBER_NAME"))
                .nickname(rs.getString("MEMBER_NICKNAME"))
                // 날짜 변환 시 null 체크를 추가하면 더 안전합니다.
                .birth(rs.getDate("MEMBER_BIRTH") != null ? rs.getDate("MEMBER_BIRTH").toLocalDate() : null)
                .state(rs.getString("MEMBER_STATE"))
                .rule(rs.getString("MEMBER_RULE")) // ADMIN 여부 판별의 핵심 필드
                .build();
    }
    // 이메일 중복 체크
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM MEMBERS WHERE MEMBER_EMAIL = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email.trim());
        return count != null && count > 0;
    }

    // 임시 비밀번호 발송
    public int updatePassword(String email, String tempPw) {
        String sql = "UPDATE MEMBERS SET MEMBER_PASSWORD = ? WHERE MEMBER_EMAIL = ?";
        return jdbcTemplate.update(sql, tempPw, email);
    }

    // 마이페이지
    public int updateMemberProfile(Members member) {
        String sql = "UPDATE MEMBERS SET MEMBER_NICKNAME = ?, MEMBER_IMGURL = ?, " +
                "MEMBER_INTRO = ?, MEMBER_ADDR = ? WHERE MEMBER_ID = ?";

        return jdbcTemplate.update(sql,
                member.getNickname(),
                member.getImgUrl(),
                member.getIntro(),
                member.getAddr(),
                member.getId());
    }
    // 이메일로 회원 정보 조회
    public Optional<Members> findByEmail(String email) {
        String sql = "SELECT * FROM MEMBERS WHERE MEMBER_EMAIL = ?";
        // query를 사용하면 결과가 없을 때 예외 대신 빈 리스트를 반환하므로 더 안전합니다.
        return jdbcTemplate.query(sql, memberRowMapper(), email.trim())
                .stream()
                .findFirst();
    }
}
