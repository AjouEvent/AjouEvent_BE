package com.example.ajouevent.service;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.ajouevent.dao.FCMTokenDao;
import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.AlarmImage;
import com.example.ajouevent.dto.ResponseDTO;
import com.example.ajouevent.dto.MemberDTO;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMService {

	private final FCMTokenDao fcmTokenDao;

	// public void sendSaleCompletedMessage(String email) {
	// 	if (!hasKey(email)) {
	// 		return;
	// 	}
	//
	// 	String token = getToken(email);
	// 	Message message = Message.builder()
	// 		.putData("title", "판매 완료 알림")
	// 		.putData("content", "등록하신 판매 입찰이 낙찰되었습니다.")
	// 		.setToken(token)
	// 		.build();
	//
	// 	send(message);
	// }

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
			.putData("click_action", url)
			.build();

		send(message);

		log.info("알림 전송 완료");

		ResponseEntity.ok().body(ResponseDTO.builder()
			.successStatus(HttpStatus.OK)
			.successContent("푸쉬 알림 성공")
			.Data(token)
			.build()
		);
	}

	// public void sendPurchaseCompletedMessage(String email) {
	// 	if (!hasKey(email)) {
	// 		return;
	// 	}
	//
	// 	String token = getToken(email);
	// 	Message message = Message.builder()
	// 		.putData("title", "구매 완료 알림")
	// 		.putData("content", "등록하신 구매 입찰이 낙찰되었습니다.")
	// 		.setToken(token)
	// 		.build();
	//
	// 	send(message);
	// }

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
