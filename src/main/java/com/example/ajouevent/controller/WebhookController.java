package com.example.ajouevent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.WebhookResponse;
import com.example.ajouevent.facade.WebhookFacade;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/api/webhook")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

	private final WebhookFacade webhookFacade;

	@PostMapping("/crawling")
	public ResponseEntity<WebhookResponse> handleWebhook(@RequestHeader("crawling-token") String token, @RequestBody NoticeDto noticeDto) {
		return webhookFacade.processWebhook(token, noticeDto);
	}
}