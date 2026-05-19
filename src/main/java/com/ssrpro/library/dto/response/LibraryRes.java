package com.ssrpro.library.dto.response;

import com.ssrpro.library.dto.entity.Library;
import lombok.*;

@Getter
@ToString
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LibraryRes {
    private Long libraryCode;
    private String libraryName;
    private String libraryAddr;
    private Double libraryLat;
    private Double libraryLon;
    /** null: API 확인 불가, true/false: 대출 가능 여부 */
    private Boolean loanAvailable;
    /** 회원 주소 기준 직선 거리(km), 정렬용 */
    private Double distanceKm;
    /** 화면 표시용 (예: 1.2km, 850m) */
    private String distanceLabel;

    public static LibraryRes of(Library library) {
        return LibraryRes.builder()
                .libraryCode(library.getLibraryCode())
                .libraryName(library.getLibraryName())
                .libraryAddr(library.getLibraryAddr())
                .libraryLat(library.getLibraryLat())
                .libraryLon(library.getLibraryLon())
                .build();
    }
}
