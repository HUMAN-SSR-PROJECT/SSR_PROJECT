package com.ssrpro.library.dto.request;


import com.ssrpro.library.dto.entity.Members;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FindIdReq {
    private String name; // 실명
    private LocalDate birth; // 생년월일

    public Members toEntity() {
        return Members.builder()
                .name(this.name)
                .birth(this.birth)
                .build();
    }
}
