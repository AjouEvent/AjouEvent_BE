package com.example.ajouevent.facade;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.WebhookResponse;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.logger.WebhookLogger;
import com.example.ajouevent.service.EventService;
import com.example.ajouevent.service.FCMService;
import com.example.ajouevent.service.PushNotificationService;
import com.example.ajouevent.service.RedisService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebhookFacade {

	private final RedisService redisService;
	private final EventService eventService;
	private final FCMService fcmService;
	private final PushNotificationService pushNotificationService;
	private final WebhookLogger webhookLogger;

	@Transactional
	public ResponseEntity<WebhookResponse> processWebhook(String token, NoticeDto noticeDto) {
		try {
			// 토큰 검증
			if (!redisService.isTokenValid("crawling-token", token)) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}

			// 공지사항 중복 여부 확인 - 공지사항 제목, 원래 공지사항 url 비교
			boolean isDuplicate = eventService.isDuplicateNotice(noticeDto.getEnglishTopic(), noticeDto.getTitle(), noticeDto.getUrl());
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
			Long eventId = eventService.postNotice(noticeDto);

			// 알림을 구독자별로 PushNotifications에 미리 저장
			Long pushClusterId = pushNotificationService.postPushNotification(noticeDto, eventId);

			// 공지사항 Topic을 구독하고있는 사용자한테 FCM 메시지 전송
			fcmService.sendNoticeNotification(noticeDto, eventId, pushClusterId);

			pushNotificationService.handleKeywordPushNotification(noticeDto, eventId);

			WebhookResponse response = WebhookResponse.builder()
				.result("Webhook processed successfully.")
				.eventId(eventId)
				.topic(noticeDto.getEnglishTopic())
				.title(noticeDto.getTitle())
				.build();
			return ResponseEntity.ok(response);

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