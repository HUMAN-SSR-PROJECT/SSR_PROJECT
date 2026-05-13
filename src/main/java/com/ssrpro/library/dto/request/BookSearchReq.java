package com.ssrpro.library.dto.request;


import com.ssrpro.library.dto.entity.Book;
import lombok.*;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookSearchReq {
    private String keyword; // 검색 키워드
    private String city; // 시.도
    private String district; //구.군

}
