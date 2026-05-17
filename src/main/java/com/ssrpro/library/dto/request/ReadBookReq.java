package com.ssrpro.library.dto.request;

import lombok.*;
import com.ssrpro.library.dto.entity.ReadBook;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReadBookReq {
    private Long bookId;
    private Double bookRating;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
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
