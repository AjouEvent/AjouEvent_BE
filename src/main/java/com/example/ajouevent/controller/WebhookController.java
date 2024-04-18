package com.example.ajouevent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.WebhookResponse;
import com.example.ajouevent.service.EventService;
import com.example.ajouevent.service.FCMService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@RequestMapping("/api/webhook")
@Slf4j
public class WebhookController {

	private final EventService eventService;
	private final FCMService fcmService;

	// go 크롤링 서버에서 보낸 웹훅 처리를 listen하는 api
	@PostMapping("/crawling")
	public ResponseEntity<WebhookResponse> handleWebhook(@RequestBody NoticeDto noticeDto) {
		// JSON 데이터를 Notice 객체로 변환 -> @RequestBody에서 직렬화 해줌
		// 크롤링한 공지사항을 DB에 저장
		eventService.postNotice(noticeDto);
		// 크롤링한 공지사항을 프론트에 뿌리기
		// 크롤링한 공지사항을 알림 전송
		// 성공적으로 처리되었음을 클라이언트에 응답
		fcmService.sendNoticeNotification(noticeDto);
		WebhookResponse webhookResponse = WebhookResponse.builder()
			.result("웹훅이 성공적으로 처리 되었습니다.")
			.topic(noticeDto.getKoreanTopic())
			.build();

		return ResponseEntity.ok().body(webhookResponse);
	}
}
