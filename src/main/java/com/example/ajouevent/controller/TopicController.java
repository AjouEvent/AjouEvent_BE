package com.example.ajouevent.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.dto.TopicDetailResponse;
import com.example.ajouevent.dto.TopicRequest;
import com.example.ajouevent.dto.TopicResponse;
import com.example.ajouevent.dto.TopicStatus;
import com.example.ajouevent.service.TopicService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/topic")
@RequiredArgsConstructor
public class TopicController {

	private final TopicService topicService;

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/subscribe")
	public ResponseEntity<ResponseDto> subscribeToTopic(@RequestBody TopicRequest topicRequest) {
		topicService.subscribeToTopics(topicRequest);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(topicRequest.getTopic() +" 토픽을 구독합니다.")
			.build()
		);
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/unsubscribe")
	public ResponseEntity<ResponseDto> unsubscribeFromTopic(@RequestBody TopicRequest topicRequest) {
		topicService.unsubscribeFromTopics(topicRequest);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(topicRequest.getTopic() +" 토픽을 구독 취소합니다.")
			.build()
		);
	}

	@PreAuthorize("isAuthenticated()")
	@DeleteMapping("/subscriptions/reset")
	public ResponseEntity<ResponseDto> resetSubscriptions() {
		topicService.resetAllSubscriptions();
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("사용자의 topic 구독을 초기화합니다.")
			.build()
		);
	}

	@GetMapping("/all")
	public List<TopicDetailResponse> getAllTopics() {
		return topicService.getAllTopics();
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/subscriptions")
	public TopicResponse getUserSubscriptions() {
		// 현재 사용자가 구독하고 있는 토픽 리스트 가져오기
		return topicService.getSubscribedTopics();
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/subscriptionsStatus")
	public List<TopicStatus> getTopicWithUserSubscriptionsStatus(Principal principal) {
		return topicService.getTopicWithUserSubscriptionsStatus(principal);
	}
}
