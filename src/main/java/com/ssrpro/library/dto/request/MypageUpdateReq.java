package com.ssrpro.library.dto.request;

import com.ssrpro.library.dto.entity.Members;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MypageUpdateReq {
  private String nickname;
  private MultipartFile imgUrl;
  private String intro;
  private String addr;

  public Members toEntity(Long memberId) {
    return Members.builder()
            .id(memberId)
            .nickname(this.nickname)
            .intro(this.intro)
            .addr(this.addr)
            .build();
  }
}
