package com.ssrpro.library.dao;

import com.ssrpro.library.dto.entity.Members;
import com.ssrpro.library.dto.request.FindIdReq;
import com.ssrpro.library.dto.request.FindPwReq;
import com.ssrpro.library.dto.request.LoginReq;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
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
                trimToEmpty(members.getEmail()), members.getPassword(), trimToEmpty(members.getName()),
                trimToEmpty(members.getNickname()), members.getBirth(),
                members.getState(), members.getRule());
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
                .imgUrl(rs.getString("MEMBER_IMGURL"))
                .intro(rs.getString("MEMBER_INTRO"))
                .addr(rs.getString("MEMBER_ADDR"))
                .createdAt(rs.getTimestamp("MEMBER_CREATED_AT") != null
                        ? rs.getTimestamp("MEMBER_CREATED_AT").toLocalDateTime() : null)
                .updatedAt(rs.getTimestamp("MEMBER_UPDATED_AT") != null
                        ? rs.getTimestamp("MEMBER_UPDATED_AT").toLocalDateTime() : null)
                .build();
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
    // 이메일 중복 체크
    public boolean existsByEmail(String email) {
        String sql = "SELECT COUNT(*) FROM MEMBERS WHERE MEMBER_EMAIL = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, email.trim());
        return count != null && count > 0;
    }

    // 닉네임 중복 체크
    public boolean existsByNickname(String nickname) {
        if (nickname == null || nickname.isBlank()) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM MEMBERS WHERE MEMBER_NICKNAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, nickname.trim());
        return count != null && count > 0;
    }

    /** 마이페이지 수정 — 본인 제외 닉네임 중복 */
    public boolean existsByNicknameExcept(Long memberId, String nickname) {
        if (nickname == null || nickname.isBlank() || memberId == null) {
            return false;
        }
        String sql = "SELECT COUNT(*) FROM MEMBERS WHERE MEMBER_NICKNAME = ? AND MEMBER_ID <> ?";
        Integer count =
            jdbcTemplate.queryForObject(sql, Integer.class, nickname.trim(), memberId);
        return count != null && count > 0;
    }

    // 마이페이지 수정
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
    // 마이페이지 상세 조회 (ID로 회원 정보 가져오기)
    public Optional<Members> findById(Long id) {
        String sql = "SELECT * FROM MEMBERS WHERE MEMBER_ID = ?";
        try {
            return Optional.ofNullable(jdbcTemplate.queryForObject(sql, memberRowMapper(), id));
        } catch (Exception e) {
            return Optional.empty();
        }
    }
    private static final String MEMBER_KEYWORD_WHERE =
            " WHERE MEMBER_EMAIL LIKE ? OR MEMBER_NAME LIKE ? OR MEMBER_NICKNAME LIKE ?"
                    + " OR TO_CHAR(MEMBER_ID) LIKE ?";

    // 관리자 전체 회원 조회 및 검색 @param keyword : 검색어 (빈값일 경우 전체 조회)
    public List<Members> findAll(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            String sql = "SELECT * FROM MEMBERS" + MEMBER_KEYWORD_WHERE + " ORDER BY MEMBER_ID DESC";
            String searchTag = "%" + keyword.trim() + "%";
            return jdbcTemplate.query(
                    sql, memberRowMapper(), searchTag, searchTag, searchTag, searchTag);
        }
        return jdbcTemplate.query("SELECT * FROM MEMBERS ORDER BY MEMBER_ID DESC", memberRowMapper());
    }

    public List<Members> findAllPaged(String keyword, int offset, int limit) {
        String inner;
        Object[] innerArgs;
        if (keyword != null && !keyword.trim().isEmpty()) {
            inner = "SELECT * FROM MEMBERS" + MEMBER_KEYWORD_WHERE + " ORDER BY MEMBER_ID DESC";
            String searchTag = "%" + keyword.trim() + "%";
            innerArgs = new Object[]{searchTag, searchTag, searchTag, searchTag};
        } else {
            inner = "SELECT * FROM MEMBERS ORDER BY MEMBER_ID DESC";
            innerArgs = new Object[0];
        }
        int endRow = offset + limit;
        String sql = "SELECT * FROM ("
                + "  SELECT m.*, ROWNUM rn FROM ("
                + inner
                + "  ) m WHERE ROWNUM <= ?"
                + ") WHERE rn > ?";
        Object[] args = new Object[innerArgs.length + 2];
        System.arraycopy(innerArgs, 0, args, 0, innerArgs.length);
        args[innerArgs.length] = endRow;
        args[innerArgs.length + 1] = offset;
        return jdbcTemplate.query(sql, memberRowMapper(), args);
    }

    public int countAll(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            String sql = "SELECT COUNT(*) FROM MEMBERS" + MEMBER_KEYWORD_WHERE;
            String searchTag = "%" + keyword.trim() + "%";
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class,
                    searchTag, searchTag, searchTag, searchTag);
            return count != null ? count : 0;
        }
        return getTotalMemberCount();
    }

    /**
     * 회원 상태 변경 (정상, 정지 등 - image_b1185e.png 관리자 기능 대응)
     */
    public int updateMemberState(Long id, String state) {
        String sql = "UPDATE MEMBERS SET MEMBER_STATE = ? WHERE MEMBER_ID = ?";
        return jdbcTemplate.update(sql, state, id);
    }

    /**
     * 회원 삭제 (관리자 기능)
     */
    public int deleteMember(Long id) {
        String sql = "DELETE FROM MEMBERS WHERE MEMBER_ID = ?";
        return jdbcTemplate.update(sql, id);
    }
    /**
     * 이메일로 회원 정보 전체 조회 (세션 최신화 및 로그인 처리에 필수)
     */
    public Optional<Members> findByEmail(String email) {
        String sql = "SELECT * FROM MEMBERS WHERE MEMBER_EMAIL = ?";
        // query를 사용하면 결과가 없을 때 예외 대신 빈 리스트를 반환하므로 .stream().findFirst()가 안전합니다.
        return jdbcTemplate.query(sql, memberRowMapper(), email.trim())
                .stream()
                .findFirst();
    }
    // 총 회원 수 조회
    public int getTotalMemberCount() {
        String sql = "SELECT COUNT(*) FROM MEMBERS";
        // 결과가 단일 숫아이므로 queryForObject가 가장 적합합니다.
        return jdbcTemplate.queryForObject(sql, Integer.class);
    }
    // 최근 가입 회원 (10명) — Oracle 11g 호환 (ROWNUM)
    public List<Members> findRecentMembers() {
        String sql = "SELECT * FROM ("
                + "  SELECT * FROM MEMBERS ORDER BY MEMBER_CREATED_AT DESC"
                + ") WHERE ROWNUM <= 10";
        return jdbcTemplate.query(sql, memberRowMapper());
    }
}
