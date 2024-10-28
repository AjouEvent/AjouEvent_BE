package com.example.ajouevent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.WebhookResponse;
import com.example.ajouevent.service.EventService;
import com.example.ajouevent.service.FCMService;
import com.example.ajouevent.service.RedisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/webhook")
@Slf4j
public class WebhookController {

	private final RedisService redisService;
	private final EventService eventService;
	private final FCMService fcmService;

	// go 크롤링 서버에서 보낸 웹훅 처리를 listen하는 api
	@PostMapping("/crawling")
	public ResponseEntity<WebhookResponse> handleWebhook(@RequestHeader("Authorization") String token, @RequestBody NoticeDto noticeDto) {

		// 토큰 검증
		if (!redisService.isTokenValid("crawling-token", token)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}

		// 크롤링한 공지사항을 DB에 저장
		Long eventId = eventService.postNotice(noticeDto);
		// 공지사항 Topic을 구독하고있는 사용자한테 FCM 메시지 전송
		return fcmService.sendNoticeNotification(noticeDto, eventId);
	}
}
