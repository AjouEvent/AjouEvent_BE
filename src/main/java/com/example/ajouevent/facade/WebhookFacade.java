package com.example.ajouevent.facade;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.PushCluster;
import com.example.ajouevent.service.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.WebhookResponse;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.logger.WebhookLogger;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookFacade {

	private final RedisService redisService;
	private final EventCommandService eventCommandService;
	private final FCMService fcmService;
	private final PushNotificationService pushNotificationService;
	private final PushClusterService pushClusterService;
	private final WebhookLogger webhookLogger;

	public ResponseEntity<WebhookResponse> processWebhook(String token, NoticeDto noticeDto) {
		// 크롤링 인증 토큰 검증
		if (!redisService.isTokenValid("crawling-token", token)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		try {
			// 공지사항 중복 여부 확인 - 공지사항 제목, 원래 공지사항 url 비교
			boolean isDuplicate = eventCommandService.isDuplicateNotice(noticeDto.getEnglishTopic(), noticeDto.getTitle(), noticeDto.getUrl());
			if (isDuplicate) {
				webhookLogger.log(String.format("Duplicate notice detected: %s, %s", noticeDto.getKoreanTopic(), noticeDto.getTitle()));
				return ResponseEntity.status(HttpStatus.CONFLICT).body(
					WebhookResponse.builder()
						.result("Duplicate notice detected. Skipping processing.")
						.topic(noticeDto.getEnglishTopic())
						.title(noticeDto.getTitle())
						.build()
				);
			}

			// 크롤링한 공지사항을 DB에 저장
			ClubEvent clubEvent = eventCommandService.postNotice(noticeDto);

			// 토픽/키워드별 PushCluster 준비: 토픽 및 키워드별 푸시 알림 전송을 위한
			PushCluster topicCluster = pushClusterService.prepareTopicCluster(noticeDto, clubEvent);
			List<PushCluster> keywordClusters = pushClusterService.prepareKeywordClusters(noticeDto, clubEvent);

			// 사용자별 푸시 알림 미리 DB에 저장
			pushNotificationService.saveTopicNotifications(topicCluster);
			pushNotificationService.saveKeywordNotifications(keywordClusters);

			if (topicCluster.getTotalCount() > 0) {
				// 사용자별 읽지 않은 알림 개수 조회
				Map<Long, Long> unreadCountMap = pushNotificationService.getUnreadNotificationCountMapForTopic(topicCluster.getTopic().getKoreanTopic());

				// FCM으로 토픽 알림 발송
				fcmService.sendTopicPush(topicCluster, unreadCountMap);
			}

			for (PushCluster keywordCluster : keywordClusters) {
				if (keywordCluster.getTotalCount() > 0) {
					// 사용자별 읽지 않은 알림 개수 조회
					Map<Long, Long> keywordUnreadCountMap = pushNotificationService.getUnreadNotificationCountMapForKeyword(keywordCluster.getKeyword().getEncodedKeyword());

					// FCM으로 키워드 알림 발송
					fcmService.sendKeywordPush(keywordCluster, keywordUnreadCountMap);
				}
			}

			return ResponseEntity.ok(WebhookResponse.builder()
							.result("Webhook processed successfully.")
							.eventId(clubEvent.getEventId())
							.topic(noticeDto.getEnglishTopic())
							.title(noticeDto.getTitle())
							.build()
			);

		} catch (CustomException e) {
			webhookLogger.log("Webhook 처리 중 오류 발생: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
				WebhookResponse.builder()
					.result("Webhook 처리 실패: " + e.getMessage())
					.topic(noticeDto.getEnglishTopic())
					.title(noticeDto.getTitle())
					.build()
			);
		}
	}
}