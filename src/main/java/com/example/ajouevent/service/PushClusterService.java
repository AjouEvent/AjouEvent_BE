package com.example.ajouevent.service;

import com.example.ajouevent.domain.*;
import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.PushClusterStatsResponse;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.logger.PushClusterLogger;
import com.example.ajouevent.repository.*;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushClusterService {

	private final PushClusterRepository pushClusterRepository;
	private final PushClusterBulkRepository pushClusterBulkRepository;
	private final PushNotificationRepository pushNotificationRepository;
	private final PushClusterTokenBulkRepository pushClusterTokenBulkRepository;
	private final TopicRepository topicRepository;
	private final TopicTokenRepository topicTokenRepository;
	private final KeywordRepository keywordRepository;
	private final KeywordTokenRepository keywordTokenRepository;
	private final RedisService redisService;
	private final PushClusterLogger pushClusterLogger;

	private static final String DEFAULT_NOTIFICATION_IMAGE_URL = "https://www.ajou.ac.kr/_res/ajou/kr/img/intro/img-symbol.png";
	private static final String CLICK_URL_PREFIX = "https://www.ajouevent.com/event/";
	private static final String DEFAULT_CLICK_ACTION_URL =  "https://www.ajouevent.com";

	@Transactional
	public PushCluster prepareTopicCluster(NoticeDto dto, ClubEvent clubEvent) {
		// 푸시 알림 메시지 구성
		String title = composeMessageTitle(dto);
		String body = composeBody(dto);
		String imageUrl = getFirstImageUrl(dto);
		String clickUrl = buildClickUrl(dto, clubEvent.getEventId());

		Topic topic = topicRepository.findByDepartment(dto.getEnglishTopic())
				.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		// Topic을 구독 중인 TopicToken 조회 (isDeleted가 false)
		List<TopicToken> topicTokens = topicTokenRepository.findByTopicWithValidTokensAndReceiveNotificationTrue(topic);

		JobStatus initialStatus = topicTokens.isEmpty() ? JobStatus.NONE : JobStatus.PENDING;

		PushCluster cluster = PushCluster.builder()
				.clubEvent(clubEvent)
				.title(title)
				.body(body)
				.imageUrl(imageUrl)
				.clickUrl(clickUrl)
				.totalCount(topicTokens.size())
				.jobStatus(initialStatus)
				.registeredAt(LocalDateTime.now())
				.topic(topic)
				.build();
		pushClusterRepository.save(cluster);

		List<PushClusterToken> clusterTokens = topicTokens.stream()
				.map(token -> PushClusterToken.builder()
						.pushCluster(cluster)
						.token(token.getToken()) // TopicToken에 연결된 Token 가져오기
						.jobStatus(JobStatus.PENDING) // 초기 상태: PENDING
						.requestTime(LocalDateTime.now())
						.build())
				.toList();
		pushClusterTokenBulkRepository.saveAll(clusterTokens);

		return cluster;
	}

	@Transactional
	public List<PushCluster> prepareKeywordClusters(NoticeDto dto, ClubEvent clubEvent) {
		List<PushCluster> clusters = new ArrayList<>();
		Topic topic = topicRepository.findByDepartment(dto.getEnglishTopic())
				.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));
		List<Keyword> keywords = keywordRepository.findByTopic(topic);

		for (Keyword keyword : keywords) {
			// 푸시 알림 메시지 구성
			String title = keyword.getKoreanKeyword() + "-" + composeMessageTitle(dto);
			String body = composeBody(dto);
			String imageUrl = getFirstImageUrl(dto);
			String clickUrl = buildClickUrl(dto, clubEvent.getEventId());

			if (dto.getTitle().contains(keyword.getKoreanKeyword())) {
				List<KeywordToken> keywordTokens = keywordTokenRepository.findKeywordTokensWithTokenByKeyword(keyword);
				JobStatus initialStatus = keywordTokens.isEmpty() ? JobStatus.NONE : JobStatus.PENDING;
				PushCluster cluster = PushCluster.builder()
						.clubEvent(clubEvent)
						.topic(topic)
						.keyword(keyword)
						.title(title)
						.body(body)
						.imageUrl(imageUrl)
						.clickUrl(clickUrl)
						.totalCount(keywordTokens.size())
						.jobStatus(initialStatus)
						.registeredAt(LocalDateTime.now())
						.build();
				pushClusterRepository.save(cluster);

				List<PushClusterToken> clusterTokens = keywordTokens.stream()
						.map(token -> PushClusterToken.builder()
								.pushCluster(cluster)
								.token(token.getToken()) // KeywordToken에 연결된 Token 가져오기
								.jobStatus(JobStatus.PENDING) // 초기 상태: PENDING
								.requestTime(LocalDateTime.now())
								.build())
						.toList();
				pushClusterTokenBulkRepository.saveAll(clusterTokens);

				clusters.add(cluster);
			}
		}
		return clusters;
	}

	private String getFirstImageUrl(NoticeDto dto) {
		return (dto.getImages() != null && !dto.getImages().isEmpty())
				? dto.getImages().get(0)
				: DEFAULT_NOTIFICATION_IMAGE_URL;
	}

	private String buildClickUrl(NoticeDto noticeDto, Long eventId) {
        return Optional.ofNullable(noticeDto.getUrl())
				.filter(u -> !u.isEmpty())
				.map(u -> CLICK_URL_PREFIX + eventId) // 알림 클릭시, 크롤링 후 DB에 저장된, 앱 상세페이지로 이동
				.orElse(DEFAULT_CLICK_ACTION_URL);
	}

	private String composeMessageTitle(NoticeDto noticeDto) {
		return String.format("[%s]", noticeDto.getKoreanTopic());
	}

	private String composeBody(NoticeDto noticeDto) {
		return noticeDto.getTitle();
	}

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