package com.ssrpro.library.dto.request;

import com.ssrpro.library.dto.entity.Members;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignUpReq {
    private String email;
    private String name;
    private String nickname;
    private LocalDate birth;
    private String password;

    public Members toEntity() {
        return Members.builder()
                .email(this.email)
                .name(this.name)
                .nickname(this.nickname)
                .birth(this.birth)
                .password(this.password)
                .state("활동")
                .rule("N")
                .build();
    }
}
