package com.example.ajouevent.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.ajouevent.dto.EventResponseDto;
import com.example.ajouevent.dto.PostEventDto;
import com.example.ajouevent.dto.PostNotificationDto;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.service.EventService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/event")
public class EventController {

	private final EventService eventService;

	// 알림 등록 - 동아리, 학생회 이벤트 + 공지사항 크롤링
	@PostMapping("/notification")
	public ResponseEntity<ResponseDto> postNotification(@RequestBody PostNotificationDto postNotificationDTO, Principal principal) {
		eventService.createNotification(postNotificationDTO, principal);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(postNotificationDTO.getAlarmDateTime() +" 에 알림 전송을 합니다.")
			.build()
		);
	}

	// 게시글 생성
	@PostMapping("/new")
	public ResponseEntity<ResponseDto> postEvent(@RequestPart(value = "data") PostEventDto postEventDto, @RequestPart(value = "image", required = false)
	List<MultipartFile> images) throws IOException {
		eventService.postEvent(postEventDto, images);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글이 성공적으로 업로드되었습니다.")
			.build()
		);
	}

	// @GetMapping("/{eventId}")
	// public EventResponseDto detail(@PathVariable("eventId") Long eventId) {
	// 	return eventService.getEvent(eventId);
	// }

	// 전체 글 보기 페이지(홈) -> 일단 테스트용으로 올린거 전부
	@GetMapping("/all")
	public List<EventResponseDto> getEventList() {
		return eventService.getEventList();
	}

	@GetMapping("/test")
	public String testGetMethod() {
		return "get";
	}
}
