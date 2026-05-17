package com.ssrpro.library.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HeaderProfileView {
  private final String imgUrl;
  private final String avatarInitial;

  public static HeaderProfileView guest() {
    return new HeaderProfileView(null, "?");
  }
}
