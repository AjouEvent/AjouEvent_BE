package com.example.ajouevent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.ajouevent.dao.FCMTokenDao;
import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.AlarmImage;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicToken;
import com.example.ajouevent.dto.LoginResponse;
import com.example.ajouevent.dto.NoticeDTO;
import com.example.ajouevent.dto.ResponseDTO;
import com.example.ajouevent.dto.MemberDTO;
import com.example.ajouevent.dto.WebhookResponse;
import com.example.ajouevent.repository.TokenRepository;
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

	private final FCMTokenDao fcmTokenDao;
	private final TopicRepository topicRepository;
	private final TopicTokenRepository topicTokenRepository;
	private final TokenRepository tokenRepository;

	public void sendEventNotification(String email, Alarm alarm) {
		if (!hasKey(email)) {
			ResponseEntity.ok().body(ResponseDTO.builder()
				.successStatus(HttpStatus.OK)
				.successContent("해당 유저가 존재하지 않습니다")
				.Data(email)
				.build()
			);
			log.info("알림 전송 실패");
			return;
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

		String token = getToken(email);
		Message message = Message.builder()
			.setToken(token)
			.setNotification(Notification.builder()
				.setTitle(title)
				.setBody(body)
				.setImage(imageUrl)
				.build())
			.putData("click_action", url) // 동아리, 학생회 이벤트는 post한 이벤트 상세 페이지로 redirection "https://ajou-event.shop/event/{eventId}
			.build();

		send(message);

		log.info(email+ "에게 알림 전송 완료");

		ResponseEntity.ok().body(ResponseDTO.builder()
			.successStatus(HttpStatus.OK)
			.successContent("푸쉬 알림 성공")
			.Data(token)
			.build()
		);
	}

	public ResponseEntity<WebhookResponse> sendNoticeNotification(NoticeDTO noticeDTO) {

		log.info("크롤링한 공지사항 date: " + noticeDTO.getDate());
		log.info("크롤링한 공지사항 title: " + noticeDTO.getTitle());
		log.info("크롤링한 공지사항 englishTopic: " + noticeDTO.getEnglishTopic());


		// 알람에서 꺼낼지 vs payload에서 꺼낼지
		String koreanTopic = noticeDTO.getKoreanTopic();
		String title = noticeDTO.getTitle(); // ex) 아주대학교 경영학과 공지사항

		// FCM 메시지 구성
		String messageTitle = "[" + koreanTopic + "]" + " " + title;

		String url = "https://ajou-event.shop"; // 실제 학교 홈페이지 공지사항으로 이동 (default는 우리 웹사이트)

		String topic = noticeDTO.getEnglishTopic();

		String body = noticeDTO.getKoreanTopic() + "공지사항입니다.";

		if (!noticeDTO.getContent().isEmpty()) {
			body = noticeDTO.getContent(); // ex) 경영인의 밤 행사 안내
		}

		log.info("body 정보: " + body);

		if (noticeDTO.getUrl() != null) {
			url = noticeDTO.getUrl();
		}

		// 기본 default 이미지는 학교 로고
		String imageUrl = "https://ajou-event-bucket.s3.ap-northeast-2.amazonaws.com/static/1e7b1dc2-ae1b-4254-ba38-d1a0e7cfa00c.20240307_170436.jpg";

		if (noticeDTO.getImages() == null || noticeDTO.getImages().isEmpty()) {
			log.info("images 리스트가 비어있습니다.");
			// images 리스트가 null 이거나 비어있을 경우, 기본 이미지 리스트를 생성하고 설정
			List<String> defaultImages = new ArrayList<>();
			defaultImages.add(imageUrl);
			noticeDTO.setImages(defaultImages);
		}

		// 이미지 URL을 첫 번째 이미지로 설정
		imageUrl = noticeDTO.getImages().get(0);

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

		log.info(topic+ "을 구독한 사람에게 알림 전송 완료");

		ResponseEntity.ok().body(ResponseDTO.builder()
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


	public void subscribeToTopic(String topicName, List<Token> tokens) {
		try {
			Optional<Topic> optionalTopic = topicRepository.findByName(topicName);
			Topic topic;
			if (optionalTopic.isPresent()) {
				topic = optionalTopic.get();
			} else {
				topic = Topic.builder()
					.name(topicName)
					.build();
			}

			List<TopicToken> topicTokens = new ArrayList<>();
			for (Token token : tokens) {
				Token existingToken = tokenRepository.findByValue(token.getValue()); // 데이터베이스에서 토큰 값으로 조회
				if (existingToken != null) {
					// 이미 존재하는 토큰인 경우 기존 토큰을 사용
					topicTokens.add(TopicToken.builder()
						.topic(topic)
						.token(existingToken)
						.build());
				} else {
					// 존재하지 않는 경우 새로운 토큰을 저장
					Token savedToken = tokenRepository.save(token);
					topicTokens.add(TopicToken.builder()
						.topic(topic)
						.token(savedToken)
						.build());
				}
			}

			topicRepository.save(topic);
			topicTokenRepository.saveAll(topicTokens);

			List<String> tokenValues = tokens.stream()
				.map(Token::getValue)
				.collect(Collectors.toList());

			TopicManagementResponse response = FirebaseMessaging.getInstance().subscribeToTopicAsync(tokenValues, topicName).get();
			System.out.println("Subscribed to topic: " + topicName);
			System.out.println(response.getSuccessCount() + " tokens were subscribed successfully");
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			// 구독에 실패한 경우에 대한 처리
		}
	}



	public void unsubscribeFromTopic(String topic, List<Token> tokens) {
		List<String> tokenValues = tokens.stream()
			.map(Token::getValue)
			.toList();
		try {
			TopicManagementResponse response = FirebaseMessaging.getInstance().unsubscribeFromTopicAsync(tokenValues, topic).get();
			System.out.println("Unsubscribed from topic: " + topic);
			System.out.println(response.getSuccessCount() + " tokens were unsubscribed successfully");
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			// 구독 해지에 실패한 경우에 대한 처리
		}
	}

	public void saveClientId(MemberDTO.LoginRequest loginRequest) {
		// String clientId = loginRequest.getToken();

		fcmTokenDao.saveToken(loginRequest); // -> 레포지토리나 redis에 유저 Id 값과 함께 저장
	}

	public void send(Message message) {
		FirebaseMessaging.getInstance().sendAsync(message);
	}

	public void saveToken(MemberDTO.LoginRequest loginRequest) {
		fcmTokenDao.saveToken(loginRequest);
	}

	public void deleteToken(String email) {
		fcmTokenDao.deleteToken(email);
	}

	private boolean hasKey(String email) {
		return fcmTokenDao.hasKey(email);
	}

	private String getToken(String email) {
		return fcmTokenDao.getToken(email);
	}
}
