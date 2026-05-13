package com.ssrpro.library.dto.entity;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Members {
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
}
