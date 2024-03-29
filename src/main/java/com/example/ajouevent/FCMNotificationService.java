package com.example.ajouevent;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.ajouevent.dto.ResponseDTO;
import com.example.ajouevent.dto.UserDTO.LoginRequest;
import com.example.ajouevent.dao.FCMTokenDao;
import com.example.ajouevent.repository.UserRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FCMNotificationService {

	private final FCMTokenDao fcmTokenDao;

	public ResponseEntity<ResponseDTO> sendEventNotification(String email) {
		if (!hasKey(email)) {
			return ResponseEntity.ok().body(ResponseDTO.builder()
				.successStatus(HttpStatus.OK)
				.successContent("해당 유저가 존재하지 않습니다")
				.Data(email)
				.build()
			);
		}

		String token = getToken(email);
		Message message = Message.builder()
			.putData("title", "아주대 소프트웨어학과 학생회 공지사항")
			.putData("content", "중간고사 간식 사업 알림")
			.setToken(token)
			.build();
		send(message);

		return ResponseEntity.ok().body(ResponseDTO.builder()
			.successStatus(HttpStatus.OK)
			.successContent("푸쉬 알림 성공")
			.Data(token)
			.build()
		);
	}

	public void send(Message message) {
		FirebaseMessaging.getInstance().sendAsync(message);
	}

	public void saveToken(LoginRequest loginRequest) {
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