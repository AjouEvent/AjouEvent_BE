package com.example.ajouevent.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PushClusterStatsResponse {
	private Long pushClusterId; // PushCluster ID
	private String title; // 푸시 제목
	private int totalTokens; // 총 발송 토큰 수
	private int successfulTokens; // 성공한 토큰 수
	private int failedTokens; // 실패한 토큰 수
	private int totalNotifications; // 총 알림 수
	private int clickedNotifications; // 클릭된 알림 수
	private double deliveryRate; // 수신률 (%)
	private double clickRate; // 클릭률 (%)
	private String url; // 푸시 랜딩 URL
	private String jobStatus; // 작업 상태

	private LocalDateTime registerAt; // 발송 등록 시간
	private LocalDateTime startAt; // 발송 시작 시간
	private LocalDateTime endAt; // 발송 완료 시간
}