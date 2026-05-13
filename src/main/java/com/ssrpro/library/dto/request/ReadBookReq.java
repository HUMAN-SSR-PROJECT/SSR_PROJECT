package com.ssrpro.library.dto.request;

import lombok.*;
import com.ssrpro.library.dto.entity.ReadBook;
import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReadBookReq {
    private Long bookId;
    private Double bookRating;
    private LocalDateTime endDate;
    private String memo;

    public ReadBook toEntity(Long memberId){
        return ReadBook.builder()
                .memberId(memberId)
                .bookId(this.bookId)
                .readBookRating(this.bookRating)
                .readBookEnd(this.endDate)
                .readBookMemo(this.memo)
                .build();
    }
}
