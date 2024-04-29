package com.example.ajouevent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.dto.TopicRequest;
import com.example.ajouevent.service.FCMService;
import com.example.ajouevent.service.TopicService;

import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/api/topic")
@RequiredArgsConstructor
public class TopicController {

	private final FCMService fcmService;
	private final TopicService topicService;

	@PostMapping("/subscribe")
	public ResponseEntity<ResponseDto> subscribeToTopic(@RequestBody TopicRequest topicRequest) {
		topicService.subscribeToTopic(topicRequest);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(topicRequest.getTopic() +" 토픽을 구독합니다.")
			.build()
		);
	}

	@PostMapping("/unsubscribe")
	public ResponseEntity<ResponseDto> unsubscribeFromTopic(@RequestBody TopicRequest topicRequest) {
		String topic = topicRequest.getTopic();
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(topic +" 토픽을 구독 취소합니다.")
			.build()
		);
	}

	@DeleteMapping("/subscriptions/reset")
	public ResponseEntity<ResponseDto> resetSubscriptions() {
		topicService.resetAllSubscriptions();
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("모든 topic 구독을 초기화합니다.")
			.build()
		);
	}
}
