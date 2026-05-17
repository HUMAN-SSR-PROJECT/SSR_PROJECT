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
    /** 정보나루 region (시·도 2자리, 예: 11=서울, 31=경기) */
    private int city;
    /** 정보나루 dtl_region (시군구 5자리, 0=전체, 예: 11010=종로구) */
    private int district;
    /** 1부터 시작 */
    private int page;

}
