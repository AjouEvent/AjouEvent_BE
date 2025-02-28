package com.example.ajouevent.controller;

import static org.springframework.data.domain.Sort.Direction.*;

import java.security.Principal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.dto.KeywordNotificationResponse;
import com.example.ajouevent.dto.NotificationClickRequest;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.dto.SliceResponse;
import com.example.ajouevent.dto.TopicNotificationResponse;
import com.example.ajouevent.dto.UnreadNotificationCountResponse;
import com.example.ajouevent.service.PushNotificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notification")
public class PushNotificationController {

	private final PushNotificationService pushNotificationService;

	// Topic 알림 조회 API
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/topic")
	public SliceResponse<TopicNotificationResponse> getTopicNotifications(@PageableDefault(sort = "notifiedAt", direction = DESC) Pageable pageable) {
		return pushNotificationService.getTopicNotificationsForMember(pageable);
	}

	// Keyword 알림 조회 API
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/keyword")
	public SliceResponse<KeywordNotificationResponse> getKeywordNotifications(@PageableDefault(sort = "notifiedAt", direction = DESC) Pageable pageable) {
		return pushNotificationService.getKeywordNotificationsForMember(pageable);
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/click")
	public ResponseEntity<ResponseDto> markNotificationAsRead(@RequestBody NotificationClickRequest notificationClickRequest, Principal principal) {
		pushNotificationService.markNotificationAsRead(notificationClickRequest);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("푸시 알림을 클릭합니다.")
			.Data(notificationClickRequest.getPushNotificationId())
			.build()
		);
	}

	@GetMapping("/unread-count")
	public UnreadNotificationCountResponse getUnreadNotificationCount(Principal principal) {
		return pushNotificationService.getUnreadNotificationCount(principal);
	}
}