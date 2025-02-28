package com.example.ajouevent.service;

import com.example.ajouevent.domain.PushCluster;
import com.example.ajouevent.domain.PushNotification;
import com.example.ajouevent.dto.PushClusterStatsResponse;
import com.example.ajouevent.logger.PushClusterLogger;
import com.example.ajouevent.repository.PushClusterBulkRepository;
import com.example.ajouevent.repository.PushClusterRepository;
import com.example.ajouevent.repository.PushNotificationRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushClusterService {

	private final PushClusterRepository pushClusterRepository;
	private final PushClusterBulkRepository pushClusterBulkRepository;
	private final PushNotificationRepository pushNotificationRepository;
	private final RedisService redisService;
	private final PushClusterLogger pushClusterLogger;

	public List<PushClusterStatsResponse> calculateAllPushClusterStats() {
		List<PushCluster> pushClusters = pushClusterRepository.findAll();

		return pushClusters.stream().map(this::	calculatePushClusterStats).collect(Collectors.toList());
	}

	private PushClusterStatsResponse calculatePushClusterStats(PushCluster pushCluster) {
		int totalTokens = pushCluster.getTotalCount();
		int successfulTokens = pushCluster.getSuccessCount();
		int failedTokens = pushCluster.getFailCount();

		// PushNotification 데이터 조회
		List<PushNotification> notifications = pushNotificationRepository.findAllByPushCluster(pushCluster);
		int totalNotifications = notifications.size();
		int clickedNotifications = (int) notifications.stream().filter(PushNotification::isRead).count();

		// 수신률 및 클릭률 계산
		double deliveryRate = totalTokens > 0 ? (successfulTokens / (double) totalTokens) * 100 : 0;
		double clickRate = totalNotifications > 0 ? (clickedNotifications / (double) totalNotifications) * 100 : 0;

		return PushClusterStatsResponse.builder()
			.pushClusterId(pushCluster.getId())
			.title(pushCluster.getTitle())
			.totalTokens(totalTokens)
			.successfulTokens(successfulTokens)
			.failedTokens(failedTokens)
			.totalNotifications(totalNotifications)
			.clickedNotifications(clickedNotifications)
			.deliveryRate(deliveryRate)
			.clickRate(clickRate)
			.url(pushCluster.getClickUrl())
			.jobStatus(pushCluster.getJobStatus().name())
			.registerAt(pushCluster.getRegisteredAt())
			.startAt(pushCluster.getStartAt())
			.endAt(pushCluster.getEndAt())
			.build();
	}

	// 수신 수 증가
	public void incrementReceived(Long pushClusterId) {
		redisService.incrementField(pushClusterId, "received");
	}

	// 클릭 수 증가
	public void incrementClicked(Long pushClusterId) {
		redisService.incrementField(pushClusterId, "clicked");
	}

	// Redis 데이터를 DB로 동기화
	@Transactional
	public void syncMetricsToDatabase() {
		pushClusterLogger.log("PushCluster metrics sync started.");

		// 모든 Redis 키를 가져옴
		Set<String> keys = redisService.getKeysByPattern("pushCluster:*");
		List<PushCluster> pushClustersToUpdate = new ArrayList<>();

		for (String key : keys) {
			// 키에서 PushCluster ID를 추출
			String[] parts = key.split(":");
			if (parts.length != 2) {
				pushClusterLogger.log("Invalid Redis key format: {}" + key);
				continue;
			}

			Long pushClusterId = Long.parseLong(parts[1]);

			// Redis에서 데이터를 가져옴
			Map<String, Integer> clusterData = redisService.getPushClusterData(pushClusterId);

			// DB 업데이트
			PushCluster pushCluster = pushClusterRepository.findById(pushClusterId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid PushCluster ID: " + pushClusterId));

			int receivedCount = clusterData.getOrDefault("received", 0);
			int clickedCount = clusterData.getOrDefault("clicked", 0);

			pushCluster.setReceivedCount(pushCluster.getReceivedCount() + receivedCount);
			pushCluster.setClickedCount(pushCluster.getClickedCount() + clickedCount);

			pushClustersToUpdate.add(pushCluster);

			// Redis 키 삭제
			redisService.deletePushCluster(pushClusterId);
		}

		// Batch Update 실행
		if (!pushClustersToUpdate.isEmpty()) {
			pushClusterBulkRepository.updateAll(pushClustersToUpdate);
		}

		pushClusterLogger.log("PushCluster metrics sync completed.");
	}


}