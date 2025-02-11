package com.example.ajouevent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.AlarmImage;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.dto.WebhookResponse;
import com.example.ajouevent.logger.AlarmLogger;
import com.example.ajouevent.logger.KeywordLogger;
import com.example.ajouevent.logger.TopicLogger;
import com.example.ajouevent.logger.WebhookLogger;
import com.example.ajouevent.repository.KeywordRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TopicRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.TopicManagementResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMService {

	private final MemberRepository memberRepository;
	private final WebhookLogger webhookLogger;
	private final TopicLogger topicLogger;
	private final KeywordLogger keywordLogger;
	private final AlarmLogger alarmLogger;

	private static final String DEFAULT_IMAGE_URL = "https://www.ajou.ac.kr/_res/ajou/kr/img/intro/img-symbol.png";
	private static final String REDIRECTION_URL_PREFIX = "https://www.ajouevent.com/event/";
	private static final String DEFAULT_CLICK_ACTION_URL =  "https://www.ajouevent.com";
	private final KeywordRepository keywordRepository;
	private final TopicRepository topicRepository;

	public void sendAlarm(String email, Alarm alarm) {
		// 사용자 조회
		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		if (member.getTokens().isEmpty()) {
			alarmLogger.log("알림 전송 실패: 토큰이 없습니다.");
		}

		String title = alarm.getSubject(); // ex) 아주대학교 소프트웨어학과 공지사항
		String body = alarm.getTitle(); // ex) IT 해외 현장 연수
		String url = "https://ajou-event.shop"; // 실제 학교 홈페이지 공지사항으로 이동 (default는 우리 웹사이트)

		if (alarm.getUrl() != null) {
			url = alarm.getUrl();
		}

		// 기본 default 이미지는 학교 로고
		String imageUrl = "https://ajou-event-bucket.s3.ap-northeast-2.amazonaws.com/static/1e7b1dc2-ae1b-4254-ba38-d1a0e7cfa00c.20240307_170436.jpg";

		if (!alarm.getAlarmImageList().isEmpty()) { // S3에서 꺼내오기 or 크롤링으로
			AlarmImage firstImage = alarm.getAlarmImageList().get(0);
			imageUrl = firstImage.getUrl();
		}


		List<Token> tokens = member.getTokens();

		alarmLogger.log(email + " 에게 알림 전송");

		for (Token token : tokens) {
			Message message = Message.builder()
				.setToken(token.getTokenValue())
				.setNotification(Notification.builder()
					.setTitle(title)
					.setBody(body)
					.setImage(imageUrl)
					.build())
				.putData("click_action", url) // 동아리, 학생회 이벤트는 post한 이벤트 상세 페이지로 redirection "https://ajou-event.shop/event/{eventId}
				.build();
			alarmLogger.log(token + " 토큰으로 알림 전송");
			send(message);
		}

		alarmLogger.log("전송하는 알림: " + title);

		ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("푸쉬 알림 성공")
			.Data(tokens)
			.build()
		);
	}

	public ResponseEntity<WebhookResponse> sendNoticeNotification(NoticeDto noticeDto, Long eventId) {
		try {
			// FCM 메시지 구성
			String messageTitle = composeMessageTitle(noticeDto);
			String topicName = noticeDto.getEnglishTopic();
			String body = composeBody(noticeDto);
			String imageUrl = getFirstImageUrl(noticeDto);
			String url = getRedirectionUrl(noticeDto, eventId);

			// FCM 메시지 생성 - topic
			Message message = createFcmMessage(topicName, messageTitle, body, imageUrl, url);
			send(message);

			// 공지사항에 해당하는 토픽을 구독 중인 모든 키워드 찾기
			Topic topic = topicRepository.findByDepartment(topicName)
				.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

			List<Keyword> keywords = keywordRepository.findByTopic(topic);

			for (Keyword keyword : keywords) {
				String koreanKeyword = keyword.getKoreanKeyword();

				// 공지사항의 제목이나 본문에 키워드가 포함되어 있는지 확인
				if (noticeDto.getTitle().contains(koreanKeyword)) {
					messageTitle = koreanKeyword + "-" + messageTitle;
					String encodedKeyword = keyword.getEncodedKeyword();
					// FCM 메시지 생성 - keyword
					Message keywordMessage = createFcmMessage(encodedKeyword, messageTitle, body, imageUrl, url);
					// 비동기적으로 알림 전송
					send(keywordMessage);

					keywordLogger.log("키워드 '영어 : " + encodedKeyword + " 한글 : " + koreanKeyword + "에 대한 공지사항이 전송되었습니다.");
				}
			}

			WebhookResponse webhookResponse = WebhookResponse.builder()
				.result("Webhook 요청이 성공적으로 처리되었습니다.")
				.topic(topicName)
				.title(noticeDto.getTitle())
				.eventId(eventId)
				.build();
			return ResponseEntity.ok().body(webhookResponse);
		} catch (Exception e) {
			String errorMessage = String.format(
				"공지사항 알림 전송 중 오류 발생 - topic: %s, title: %s, error: %s",
				noticeDto.getEnglishTopic(), noticeDto.getTitle(), e.getMessage()
			);

			webhookLogger.log(errorMessage); // webhookLogger로 로그 남김

			return ResponseEntity.status(CustomErrorCode.TOPIC_NOTIFICATION_FAILED.getStatusCode()).body(
				WebhookResponse.builder()
					.result("Webhook 요청 처리 중 오류가 발생했습니다.")
					.topic(noticeDto.getEnglishTopic())
					.title(noticeDto.getTitle())
					.eventId(eventId)
					.build()
			);
		}
	}

	private String composeMessageTitle(NoticeDto noticeDto) {
		return String.format("[%s]", noticeDto.getKoreanTopic());
	}

	private String composeBody(NoticeDto noticeDto) {
		return noticeDto.getTitle();
	}

	private String getFirstImageUrl(NoticeDto noticeDto) {
		List<String> images = Optional.ofNullable(noticeDto.getImages())
			.filter(imgs -> !imgs.isEmpty())
			.orElseGet(() -> {
				List<String> defaultImages = new ArrayList<>();
				defaultImages.add(DEFAULT_IMAGE_URL);
				return defaultImages;
			});
		return images.get(0);
	}

	private String getRedirectionUrl(NoticeDto noticeDto, Long eventId) {
		String url = Optional.ofNullable(noticeDto.getUrl())
			.filter(u -> !u.isEmpty())
			.map(u -> REDIRECTION_URL_PREFIX + eventId) // 크롤링 후 DB에 저장된, 우리 앱 상세페이지로 이동
			.orElse(DEFAULT_CLICK_ACTION_URL);
		webhookLogger.log("리다이렉션하는 URL: " + url);
		return url;
	}


	private Message createFcmMessage(String englishTopic, String messageTitle, String body,
		String imageUrl, String url) {
		return Message.builder()
			.setTopic(englishTopic)
			.setNotification(Notification.builder()
				.setTitle(messageTitle)
				.setBody(body)
				.setImage(imageUrl)
				.build())
			.putData("click_action", url) // 동아리, 학생회 이벤트는 post한 이벤트 상세 페이지로 redirection "https://ajou-event.shop/event/{eventId}
			.build();
	}


	public void subscribeToTopic(String topicName, List<String> tokens) {
		try {
			TopicManagementResponse response = FirebaseMessaging.getInstance().subscribeToTopicAsync(tokens, topicName).get();
			topicLogger.log("Subscribed to topic: " + topicName);
			topicLogger.log(response.getSuccessCount() + " tokens were subscribed successfully");
			if (response.getFailureCount() > 0) {
				topicLogger.log(response.getFailureCount() + " tokens failed to subscribe");
				response.getErrors().forEach(error -> {
					String failedToken = tokens.get(error.getIndex());
					topicLogger.log("Error for token at index " + error.getIndex() + ": " + error.getReason() + " (Token: " + failedToken + ")");
				});
			}
		} catch (InterruptedException | ExecutionException e) {
			// 구독에 실패한 경우에 대한 처리
			throw new CustomException(CustomErrorCode.SUBSCRIBE_FAILED);

		}
	}

	public void unsubscribeFromTopic(String topicName, List<String> tokens) {
		try {
			TopicManagementResponse response = FirebaseMessaging.getInstance().unsubscribeFromTopicAsync(tokens, topicName).get();
			topicLogger.log("Unsubscribed to topic: " + topicName);
			topicLogger.log(response.getSuccessCount() + " tokens were unsubscribed successfully");
			if (response.getFailureCount() > 0) {
				topicLogger.log(response.getFailureCount() + " tokens failed to unsubscribe");
				response.getErrors().forEach(error -> {
					String failedToken = tokens.get(error.getIndex());
					topicLogger.log("Error for token at index " + error.getIndex() + ": " + error.getReason() + " (Token: " + failedToken + ")");
				});
			}
		} catch (InterruptedException | ExecutionException e) {
			// 구독 해지에 실패한 경우에 대한 처리
			throw new CustomException(CustomErrorCode.SUBSCRIBE_CANCEL_FAILED);
		}
	}

	public void send(Message message) {
		FirebaseMessaging.getInstance().sendAsync(message);
	}
}
