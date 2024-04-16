package com.example.ajouevent.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.dto.ResponseDTO;
import com.example.ajouevent.dto.TopicRequest;
import com.example.ajouevent.repository.TopicRepository;
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
	public ResponseEntity<ResponseDTO> subscribeToTopic(@RequestBody TopicRequest topicRequest) {
		String topicName = topicRequest.getTopic();
		List<Token> tokens = topicRequest.getTokens();

		// topicService.subscribeToTopic(topicRequest);
		fcmService.subscribeToTopic(topicName, tokens);
		return ResponseEntity.ok().body(ResponseDTO.builder()
			.successStatus(HttpStatus.OK)
			.successContent(topicName +" 토픽을 구독합니다.")
			.build()
		);
	}

	@PostMapping("/unsubscribe")
	public ResponseEntity<ResponseDTO> unsubscribeFromTopic(@RequestBody TopicRequest topicRequest) {
		String topic = topicRequest.getTopic();
		List<Token> tokens = topicRequest.getTokens();
		fcmService.unsubscribeFromTopic(topic, tokens);
		return ResponseEntity.ok().body(ResponseDTO.builder()
			.successStatus(HttpStatus.OK)
			.successContent(topic +" 토픽을 구독 취소합니다.")
			.build()
		);
	}
}
