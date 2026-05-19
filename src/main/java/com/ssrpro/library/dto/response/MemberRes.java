package com.ssrpro.library.dto.response;

import com.ssrpro.library.dto.entity.Members;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class MemberRes {
    private Long id;
    private String nickname;
    private String email;
    private String name;
    private LocalDate birth;
    private String rule;
    private String imgUrl;
    private String addr;
    private String intro;
    private String state;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static MemberRes from(Members members) {
        return MemberRes.builder()
                .id(members.getId())
                .nickname(members.getNickname())
                .email(members.getEmail())
                .name(members.getName())
                .birth(members.getBirth())
                .rule(members.getRule())
                .imgUrl(members.getImgUrl())
                .addr(members.getAddr())
                .intro(members.getIntro())
                .state(members.getState())
                .createdAt(members.getCreatedAt())
                .updatedAt(members.getUpdatedAt())
                .build();
    }
}
