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
    private String libraryName;
    private String libraryAddr;
    // 추후 API 및 로직 으로 인해 추가예정

    public static LibraryRes of(Library library){
        return LibraryRes.builder()
                .libraryName(library.getLibraryName())
                .libraryAddr(library.getLibraryAddr())
                .build();
    }
}
