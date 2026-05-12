package com.ssrpro.library.dto.entity;

import jakarta.validation.Valid;
import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "REVIEW")
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 1. 접근 제한자 변경
@Valid
public class Review {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "REVIEW_ID")
	private Long reviewId;

	@Column(name = "BOOK_ID", nullable = false)
	private Long bookId;

	@Column(name = "MEMBER_ID", nullable = false)
	private Long memberId;

	@Column(name = "REVIEW_COMMENT", length = 200)
	private String reviewComment;

	@Column(name = "REVIEW_RATING", precision = 2, scale = 1)
	private Double reviewRating;

	@Column(name = "REVIEW_CREATED_AT", nullable = false, updatable = false) // 2. 업데이트 방지 설정
	private LocalDateTime reviewCreatedAt;

	@Column(name = "REVIEW_UPDATED_AT")
	private LocalDateTime reviewUpdatedAt;

	@PrePersist
	protected void onCreate() {
		this.reviewCreatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.reviewUpdatedAt = LocalDateTime.now();
	}
}