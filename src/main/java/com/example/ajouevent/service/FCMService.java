package com.example.ajouevent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutionException;

import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.View;

import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.AlarmImage;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.dto.WebhookResponse;
import com.example.ajouevent.exception.UserNotFoundException;
import com.example.ajouevent.logger.AlarmLogger;
import com.example.ajouevent.logger.NotificationLogger;
import com.example.ajouevent.logger.TopicLogger;
import com.example.ajouevent.logger.WebhookLogger;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TokenRepository;
import com.example.ajouevent.repository.TopicMemberRepository;
import com.example.ajouevent.repository.TopicRepository;
import com.example.ajouevent.repository.TopicTokenRepository;
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

	private final TokenRepository tokenRepository;
	private final MemberRepository memberRepository;
	private final TopicTokenRepository topicTokenRepository;
	private final TopicMemberRepository topicMemberRepository;
	private final TopicRepository topicRepository;
	private final WebhookLogger webhookLogger;
	private final TopicLogger topicLogger;
	private final NotificationLogger notificationLogger;
	private final AlarmLogger alarmLogger;
	private final View error;

	public void sendEventNotification(String email, Alarm alarm) {
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

		log.info("크롤링한 공지사항 date: " + noticeDto.getDate());
		log.info("크롤링한 공지사항 title: " + noticeDto.getTitle());
		log.info("크롤링한 공지사항 englishTopic: " + noticeDto.getEnglishTopic());
		log.info("크롤링한 공지사항 url: " + noticeDto.getUrl());

		// 알람에서 꺼낼지 vs payload에서 꺼낼지
		String koreanTopic = noticeDto.getKoreanTopic();
		String title = noticeDto.getTitle(); // ex) 아주대학교 경영학과 공지사항

		// FCM 메시지 구성
		String messageTitle = "[" + koreanTopic + "]" + " " + title;

		String url = "https://ajou-event.vercel.app"; // 실제 학교 홈페이지 공지사항으로 이동 (default는 우리 웹사이트)

		String topic = noticeDto.getEnglishTopic();

		String body = noticeDto.getKoreanTopic() + "공지사항입니다.";

		if (!noticeDto.getContent().isEmpty()) {
			body = noticeDto.getContent(); // ex) 경영인의 밤 행사 안내
		}

		log.info("body 정보: " + body);

		if (noticeDto.getUrl() != null) {
			url = "https://ajou-event.vercel.app/event/" + eventId;
			webhookLogger.log("리다이렉션하는 url : " + url);
		}

		// 기본 default 이미지는 학교 로고
		String imageUrl = "https://ajou-event-bucket.s3.ap-northeast-2.amazonaws.com/static/1e7b1dc2-ae1b-4254-ba38-d1a0e7cfa00c.20240307_170436.jpg";

		if (noticeDto.getImages() == null || noticeDto.getImages().isEmpty()) {
			// images 리스트가 null 이거나 비어있을 경우, 기본 이미지 리스트를 생성하고 설정

			List<String> defaultImages = new ArrayList<>();
			defaultImages.add(imageUrl);
			noticeDto.setImages(defaultImages);
		}

		// 이미지 URL을 첫 번째 이미지로 설정
		imageUrl = noticeDto.getImages().get(0);

		log.info("가져온 이미지 URL: " + imageUrl);

		Message message = Message.builder()
			.setTopic(topic)
			.setNotification(Notification.builder()
				.setTitle(messageTitle)
				.setBody(body)
				.setImage(imageUrl)
				.build())
			.putData("click_action", url) // 동아리, 학생회 이벤트는 post한 이벤트 상세 페이지로 redirection "https://ajou-event.shop/event/{eventId}
			.build();

		send(message);

		webhookLogger.log("크롤링 한 제목 : " + title);
		webhookLogger.log(topic + "을 구독한 사람에게 알림 전송 완료");

		// englishTopic 값을 사용하여 해당하는 Topic 객체를 가져옴
		Topic topicEntity = topicRepository.findByDepartment(noticeDto.getEnglishTopic())
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));


		// TopicMemberRepository를 사용하여 해당 topic을 구독하는 멤버 목록을 가져옴
		List<TopicMember> topicMembers = topicMemberRepository.findByTopic(topicEntity);

		// 멤버 목록에서 각 멤버의 토큰을 가져와 로그로 출력
		for (TopicMember topicMember : topicMembers) {
			Member member = topicMember.getMember();
			List<Token> tokens = member.getTokens();
			webhookLogger.log("해당 " + topic + "을 구독하는 유저 이메일 " + member.getEmail());

			for (Token token : tokens) {
				webhookLogger.log(token.getTokenValue());
			}
			webhookLogger.log("\n");
		}




		ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("푸쉬 알림 성공")
			.Data(topic)
			.build()
		);

		WebhookResponse webhookResponse = WebhookResponse.builder()
			.result("Webhook 요청이 성공적으로 처리되었습니다.")
			.topic(topic)
			.build();
		return ResponseEntity.ok().body(webhookResponse);
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
			throw new CustomException(CustomErrorCode.SUBSCRIBE_FAILED);
			// 구독에 실패한 경우에 대한 처리
		}
	}

	public void unsubscribeFromTopic(String topic, List<String> tokens) {
		try {
			TopicManagementResponse response = FirebaseMessaging.getInstance().unsubscribeFromTopicAsync(tokens, topic).get();
			topicLogger.log("Unsubscribed to topic: " + topic);
			topicLogger.log(response.getSuccessCount() + " tokens were unsubscribed successfully");
			if (response.getFailureCount() > 0) {
				topicLogger.log(response.getFailureCount() + " tokens failed to unsubscribe");
				response.getErrors().forEach(error -> {
					String failedToken = tokens.get(error.getIndex());
					topicLogger.log("Error for token at index " + error.getIndex() + ": " + error.getReason() + " (Token: " + failedToken + ")");
				});
			}
		} catch (InterruptedException | ExecutionException e) {
			throw new CustomException(CustomErrorCode.SUBSCRIBE_CANCEL_FAILED);
			// 구독 해지에 실패한 경우에 대한 처리
		}
	}

	public void send(Message message) {
		FirebaseMessaging.getInstance().sendAsync(message);
	}

	// public void saveFCMToken(MemberDto.LoginRequest loginRequest) {
	// 	log.info("saveFCMToken 메서드 호출");
	// 	Member member = memberRepository.findByEmail(loginRequest.getEmail()).orElseThrow(NoSuchElementException::new);
	//
	// 	// Check if the token already exists
	// 	Optional<Token> existingToken = tokenRepository.findByValueAndMember(loginRequest.getFcmToken(), member);
	// 	if (existingToken.isPresent()) {
	// 		log.info("이미 존재하는 토큰: " + existingToken.get().getValue());
	// 	} else {
	// 		// Only create and save a new token if it does not exist
	// 		Token token = Token.builder()
	// 			.value(loginRequest.getFcmToken())
	// 			.member(member)
	// 			.build();
	// 		log.info("DB에 저장하는 token : " + token.getValue());
	// 		tokenRepository.save(token);
	//
	// 		// 사용자가 구독 중인 모든 토픽을 가져옴
	// 		List<TopicMember> topicMembers = topicMemberRepository.findByMember(member);
	// 		List<Topic> subscribedTopics = topicMembers.stream()
	// 			.map(TopicMember::getTopic)
	// 			.distinct()
	// 			.collect(Collectors.toList());
	//
	// 		// 새 토큰을 기존에 구독된 모든 토픽과 매핑하여 TopicToken 생성 및 저장
	// 		List<TopicToken> newSubscriptions = subscribedTopics.stream()
	// 			.map(topic -> new TopicToken(topic, token))
	// 			.collect(Collectors.toList());
	// 		topicTokenRepository.saveAll(newSubscriptions);
	//
	// 		// 각 토픽에 대해 새 토큰 구독 처리
	// 		for (Topic topic : subscribedTopics) {
	// 			subscribeToTopic(topic.getDepartment(), Collections.singletonList(token.getValue()));
	// 			log.info("새 토큰으로 " + topic.getDepartment() + " 토픽을 다시 구독합니다.");
	// 		}
	// 	}
	// }

	// public void deleteToken(String email) {
	// 	topicTokenRepository.deleteToken(email);
	// }
	//
	// private boolean hasKey(String email) {
	// 	return fcmTokenDao.hasKey(email);
	// }
	//
	// private String getToken(String email) {
	// 	return fcmTokenDao.getToken(email);
	// }
}
