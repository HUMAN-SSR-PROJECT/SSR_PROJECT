package com.ssrpro.library.dto.request;

import lombok.*;

import java.util.List;

@Setter
@Getter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BookDetailReq {
    private Long bookId;
    private List<String> libraryCodes;
}
