package com.ssrpro.library.dto.request;


import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookSearchReq {
    private String keyword;
    private String city; // 시.도
    private String district; //구.군

}
