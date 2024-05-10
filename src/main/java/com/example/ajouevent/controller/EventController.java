package com.example.ajouevent.controller;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Calendar;
import java.util.List;

import com.example.ajouevent.dto.*;
import com.example.ajouevent.service.CalendarService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.ajouevent.service.EventService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/event")
public class EventController {

	private final EventService eventService;
	private final CalendarService calendarService;

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

	@GetMapping("/type/{type}")
	public List<EventResponseDto> getEventTypeList(@PathVariable String type) {
		return eventService.getEventTypeList(type);
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/calendar")
	public void testGetMethod(@RequestBody CalendarStoreDto calendarStoreDto, Principal principal) throws GeneralSecurityException, IOException {
		calendarService.GoogleAPIClient(calendarStoreDto, principal);
    }

}
