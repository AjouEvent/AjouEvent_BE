package com.example.ajouevent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.dto.NoticeDTO;
import com.example.ajouevent.dto.ResponseDTO;
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
	public ResponseEntity<WebhookResponse> handleWebhook(@RequestBody NoticeDTO noticeDTO) {
		try {
			// JSON 데이터를 Notice 객체로 변환 -> @RequestBody에서 직렬화 해줌

			// 크롤링한 공지사항을 DB에 저장
			eventService.postNotice(noticeDTO);

			// 크롤링한 공지사항을 프론트에 뿌리기

			// 크롤링한 공지사항을 알림 전송
			// 성공적으로 처리되었음을 클라이언트에 응답
			return fcmService.sendNoticeNotification(noticeDTO);
		} catch (Exception e) {
			// 처리 중 오류가 발생한 경우 클라이언트에 에러 응답
			WebhookResponse webhookResponse = WebhookResponse.builder()
				.result("웹훅 처리 중 오류가 발생했습니다.")
				.topic(noticeDTO.getKoreanTopic())
				.build();
			return ResponseEntity.ok().body(webhookResponse);
		}
	}
}
