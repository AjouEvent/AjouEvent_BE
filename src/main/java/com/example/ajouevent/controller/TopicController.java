package com.example.ajouevent.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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

	private final TopicService topicService;

	@PostMapping("/subscribe")
	public ResponseEntity<ResponseDto> subscribeToTopic(@RequestBody TopicRequest topicRequest) {
		topicService.subscribeToTopics(topicRequest);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(topicRequest.getTopic() +" 토픽을 구독합니다.")
			.build()
		);
	}

	@PostMapping("/unsubscribe")
	public ResponseEntity<ResponseDto> unsubscribeFromTopic(@RequestBody TopicRequest topicRequest) {
		topicService.unsubscribeFromTopics(topicRequest);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(topicRequest.getTopic() +" 토픽을 구독 취소합니다.")
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

	@GetMapping("/subscriptions")
	public ResponseEntity<ResponseDto> getUserSubscriptions() {
		// 현재 사용자가 구독하고 있는 토픽 리스트 가져오기
		List<String> topics = topicService.getSubscribedTopics();

		// 결과가 비어 있는 경우 처리
		if (topics.isEmpty()) {
			return ResponseEntity.ok().body(ResponseDto.builder()
				.successStatus(HttpStatus.OK)
				.successContent("구독 중인 토픽이 없습니다.")
				.build());
		}

		// 구독 중인 토픽 목록을 문자열로 결합하여 반환
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("구독 중인 토픽: " + String.join(", ", topics))
			.build());
	}

}
