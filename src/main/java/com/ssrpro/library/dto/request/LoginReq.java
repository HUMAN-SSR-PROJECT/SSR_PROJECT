package com.ssrpro.library.dto.request;

import com.ssrpro.library.dto.entity.Members;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginReq {
    private String email;
    private String password;
}
