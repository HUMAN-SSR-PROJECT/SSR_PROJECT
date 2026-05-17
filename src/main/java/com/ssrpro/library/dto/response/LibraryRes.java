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
