package com.ssrpro.library.dto.entity;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Library {
    private Long libraryId;
    private Long libraryCode;
    private String libraryName;
    private String libraryAddr;
    private Double libraryLat;
    private Double libraryLon;
    private LocalDateTime libraryCreatedAt;
    private LocalDateTime libraryUpdatedAt;
}
