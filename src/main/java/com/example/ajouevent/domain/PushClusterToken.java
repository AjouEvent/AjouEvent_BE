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
@Table(name = "push_cluster_token")
public class PushClusterToken extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "push_cluster_id", nullable = false)
	private PushCluster pushCluster;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "token_id", nullable = false)
	private Token token;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private JobStatus jobStatus = JobStatus.PENDING; // 토큰 상태 (초기값: PENDING), SUCCESS, FAIL

	@Column(nullable = false)
	private LocalDateTime requestTime; // 발송 요청 시간

	@Column(nullable = true)
	private LocalDateTime processedTime; // 발송 처리 시간

	@Builder
	private PushClusterToken(PushCluster pushCluster, Token token, JobStatus jobStatus,
		LocalDateTime requestTime, LocalDateTime processedTime) {
		this.pushCluster = pushCluster;
		this.token = token;
		this.jobStatus = jobStatus;
		this.requestTime = requestTime;
		this.processedTime = processedTime;
	}

	public static PushClusterToken create(PushCluster cluster, Token token) {
		return PushClusterToken.builder()
			.pushCluster(cluster)
			.token(token)
			.jobStatus(JobStatus.PENDING) // 초기 상태
			.requestTime(LocalDateTime.now())
			.build();
	}

	public void markAsSending() {
		this.jobStatus = JobStatus.IN_PROGRESS;
		this.processedTime = LocalDateTime.now();
	}

	public void markAsSuccess() {
		this.jobStatus = JobStatus.SUCCESS;
		this.processedTime = LocalDateTime.now();
	}

	public void markAsFail() {
		this.jobStatus = JobStatus.FAIL;
		this.processedTime = LocalDateTime.now();
	}
}