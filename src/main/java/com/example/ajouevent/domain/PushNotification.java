package com.example.ajouevent.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushNotification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "push_cluster_id", nullable = false)
	private PushCluster pushCluster; // 발송 작업과 연결

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "topic_id", nullable = true)
	private Topic topic;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "keyword_id", nullable = true)
	private Keyword keyword;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private NotificationType notificationType; // 알림 유형 (TOPIC, KEYWORD)

	@Column(nullable = false)
	private String title; // 알림 제목

	@Column(nullable = false)
	private String body; // 알림 내용

	@Column(nullable = false)
	private String imageUrl; // 알림 이미지 URL

	@Column(nullable = false)
	private String clickUrl; // 알림 클릭 시 이동 URL

	@Column(nullable = false)
	private boolean isRead = false; // 알림 클릭 여부

	@Column(nullable = true)
	private LocalDateTime clickedAt; // 알림 클릭 시간

	@Column(nullable = true)
	private LocalDateTime notifiedAt; // 알림 전달 시간

	public void markAsRead() {
		this.isRead = true;
		this.clickedAt = LocalDateTime.now();
	}
}