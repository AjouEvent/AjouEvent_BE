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
import jakarta.persistence.Table;
import lombok.AccessLevel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "push_notification")
public class PushNotification extends BaseTimeEntity {
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

	@Builder
	private PushNotification(PushCluster pushCluster, Topic topic, Keyword keyword, Member member,
		NotificationType notificationType, String title, String body,
		String imageUrl, String clickUrl, LocalDateTime notifiedAt) {
		this.pushCluster = pushCluster;
		this.topic = topic;
		this.keyword = keyword;
		this.member = member;
		this.notificationType = notificationType;
		this.title = title;
		this.body = body;
		this.imageUrl = imageUrl;
		this.clickUrl = clickUrl;
		this.isRead = false;
		this.notifiedAt = notifiedAt;
	}

	public static PushNotification createForTopic(PushCluster cluster, Member member) {
		return PushNotification.builder()
			.pushCluster(cluster)
			.member(member)
			.topic(cluster.getTopic())
			.title(cluster.getTitle())
			.body(cluster.getBody())
			.imageUrl(cluster.getImageUrl())
			.clickUrl(cluster.getClickUrl())
			.notificationType(NotificationType.TOPIC)
			.notifiedAt(LocalDateTime.now())
			.build();
	}

	public static PushNotification createForKeyword(PushCluster cluster, Member member) {
		return PushNotification.builder()
			.pushCluster(cluster)
			.member(member)
			.keyword(cluster.getKeyword())
			.topic(cluster.getTopic())
			.title(cluster.getTitle())
			.body(cluster.getBody())
			.imageUrl(cluster.getImageUrl())
			.clickUrl(cluster.getClickUrl())
			.notificationType(NotificationType.KEYWORD)
			.notifiedAt(LocalDateTime.now())
			.build();
	}

	public void markAsRead() {
		this.isRead = true;
		this.clickedAt = LocalDateTime.now();
	}
}