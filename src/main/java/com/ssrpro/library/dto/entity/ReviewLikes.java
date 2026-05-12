package com.ssrpro.library.dto.entity;

import jakarta.validation.Valid;
import lombok.*;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "REVIEW_LIKES")
@IdClass(ReviewLikes.ReviewLikesId.class) // 내부 클래스를 참조
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Valid
public class ReviewLikes {

	@Id
	@Column(name = "MEMBER_ID")
	private Long memberId;

	@Id
	@Column(name = "REVIEW_ID")
	private Long reviewId;

	@Column(name = "REVIEW_LIKES_CREATED_AT", nullable = false, updatable = false)
	private LocalDateTime reviewLikesCreatedAt;

	@Column(name = "REVIEW_LIKES_UPDATED_AT")
	private LocalDateTime reviewLikesUpdatedAt;

	@PrePersist
	protected void onCreate() {
		this.reviewLikesCreatedAt = LocalDateTime.now();
	}

	@PreUpdate
	protected void onUpdate() {
		this.reviewLikesUpdatedAt = LocalDateTime.now();
	}

	// --- 💡 파일 하나에 복합 키 넣기 위한 내부 클래스 ---
	@NoArgsConstructor
	@AllArgsConstructor
	@EqualsAndHashCode
	public static class ReviewLikesId implements Serializable {
		private Long memberId;
		private Long reviewId;
	}
}