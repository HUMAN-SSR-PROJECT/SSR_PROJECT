package com.ssrpro.library.dto.response;

import jakarta.validation.Valid;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Valid
public class ReviewLikesRes {

	private Long memberId;
	private Long reviewId;
	private LocalDateTime reviewLikesCreatedAt;
	private LocalDateTime reviewLikesUpdatedAt;

}