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
    private int city; // 시.도
    private int district; //구.군

}
