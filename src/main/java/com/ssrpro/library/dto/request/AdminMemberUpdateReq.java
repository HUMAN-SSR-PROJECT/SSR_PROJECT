package com.ssrpro.library.dto.request;

import lombok.*;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminMemberUpdateReq {
  private Long memberId;
  private String state;
}