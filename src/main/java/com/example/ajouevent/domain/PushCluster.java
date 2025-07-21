package com.example.ajouevent.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "push_cluster")
public class PushCluster extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "club_event_id") // 연관된 이벤트
	private ClubEvent clubEvent;

	@Column(nullable = false)
	private String title; // 푸시 알림 제목

	@Column(nullable = false)
	private String body; // 푸시 알림 내용

	@Column(nullable = false)
	private String imageUrl; // 알림 이미지 URL

	@Column(nullable = false)
	private String clickUrl; // 알림 클릭 시 이동 URL

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JobStatus jobStatus = JobStatus.PENDING; // 작업 상태 (초기값: PENDING), SUCCESS, FAIL

	@Column(nullable = false)
	private int totalCount = 0; // 총 발송 토큰 수

	@Column(nullable = false)
	private LocalDateTime registeredAt; // 푸시 클러스터 등록 시간

	@Column(nullable = false)
	private int successCount = 0; // 성공한 토큰 수

	@Column(nullable = false)
	private int failCount = 0; // 실패한 토큰 수

	@Column(nullable = false)
	private int receivedCount = 0; // 수신 수

	@Column(nullable = false)
	private int clickedCount = 0; // 클릭 수

	@Column(nullable = true)
	private LocalDateTime startAt = LocalDateTime.now(); // 작업 시작 시간

	@Column(nullable = true)
	private LocalDateTime endAt = LocalDateTime.now(); // 작업 종료 시간

	@OneToMany(mappedBy = "pushCluster", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<PushClusterToken> tokens = new ArrayList<>(); // 발송 작업에 포함된 토큰들

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "topic_id")
	private Topic topic;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "keyword_id")
	private Keyword keyword;

	@Builder
	private PushCluster(ClubEvent clubEvent, String title, String body, String imageUrl, String clickUrl,
		JobStatus jobStatus, int totalCount, LocalDateTime registeredAt, int successCount, int failCount,
		int receivedCount, int clickedCount, LocalDateTime startAt, LocalDateTime endAt,
		Topic topic, Keyword keyword) {
		this.clubEvent = clubEvent;
		this.title = title;
		this.body = body;
		this.imageUrl = imageUrl;
		this.clickUrl = clickUrl;
		this.jobStatus = jobStatus;
		this.totalCount = totalCount;
		this.registeredAt = registeredAt;
		this.successCount = successCount;
		this.failCount = failCount;
		this.receivedCount = receivedCount;
		this.clickedCount = clickedCount;
		this.startAt = startAt;
		this.endAt = endAt;
		this.topic = topic;
		this.keyword = keyword;
	}

	// Topic 기반 푸시 클러스터 생성
	public static PushCluster createForTopic(ClubEvent clubEvent, Topic topic,
		String title, String body, String imageUrl, String clickUrl,
		int totalCount, JobStatus jobStatus) {
		return PushCluster.builder()
			.clubEvent(clubEvent)
			.topic(topic)
			.title(title)
			.body(body)
			.imageUrl(imageUrl)
			.clickUrl(clickUrl)
			.totalCount(totalCount)
			.jobStatus(jobStatus)
			.registeredAt(LocalDateTime.now())
			.startAt(LocalDateTime.now())
			.endAt(LocalDateTime.now())
			.successCount(0)
			.failCount(0)
			.receivedCount(0)
			.clickedCount(0)
			.build();
	}

	// Keyword 기반 푸시 클러스터 생성
	public static PushCluster createForKeyword(ClubEvent clubEvent, Topic topic, Keyword keyword,
		String title, String body, String imageUrl, String clickUrl,
		int totalCount, JobStatus jobStatus) {
		return PushCluster.builder()
			.clubEvent(clubEvent)
			.topic(topic)
			.keyword(keyword)
			.title(title)
			.body(body)
			.imageUrl(imageUrl)
			.clickUrl(clickUrl)
			.totalCount(totalCount)
			.jobStatus(jobStatus)
			.registeredAt(LocalDateTime.now())
			.startAt(LocalDateTime.now())
			.endAt(LocalDateTime.now())
			.successCount(0)
			.failCount(0)
			.receivedCount(0)
			.clickedCount(0)
			.build();
	}

	// 작업 시작 기록
	public void markAsInProgress() {
		this.startAt = LocalDateTime.now();
		this.jobStatus = JobStatus.IN_PROGRESS;
	}

	// 성공, 실패 카운트와 작업 상태를 변경
	public void updateCountsAndStatus(int successCount, int failCount) {
		this.successCount += successCount; // 배치 처리라서 +로 (누적 처리)
		this.failCount += failCount; // 배치 처리라서 +로 (누적 처리)
		if (successCount == 0 && failCount == 0) {
			this.jobStatus = JobStatus.NONE; // 보낼 대상이 없었음
		} else if (failCount > 0) {
			this.jobStatus = JobStatus.PARTIAL_FAIL; // 일부 실패
		} else {
			this.jobStatus = JobStatus.SUCCESS; // 전체 성공
		}

		this.endAt = LocalDateTime.now();
	}

	public void addReceivedCount(int count) {
		this.receivedCount += count;
	}

	public void addClickedCount(int count) {
		this.clickedCount += count;
	}
}