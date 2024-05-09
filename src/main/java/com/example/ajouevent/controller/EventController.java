package com.example.ajouevent.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.ajouevent.dto.EventDetailResponseDto;
import com.example.ajouevent.dto.EventResponseDto;
import com.example.ajouevent.dto.PostEventDto;
import com.example.ajouevent.dto.PostNotificationDto;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.dto.UpdateEventRequest;
import com.example.ajouevent.service.EventService;

import jakarta.validation.Valid;
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

	// 게시글 수정 - 데이터
	@PatchMapping("/{eventId}/data")
	public ResponseEntity<ResponseDto> updateEventData(@PathVariable("eventId") Long eventId,
		@RequestBody UpdateEventRequest request) {
		eventService.updateEventData(eventId, request);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글 데이터가 수정되었습니다.")
			.build()
		);
	}

	// 게시글 수정 - 이미지
	@PatchMapping("/{eventId}/images")
	public ResponseEntity<ResponseDto> updateEventImages(@PathVariable("eventId") Long eventId,
		@RequestPart("image") List<MultipartFile> images) throws IOException {
		eventService.updateEventImages(eventId, images);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글 이미지가 수정되었습니다.")
			.build());
	}


	// 게시글 삭제
	@DeleteMapping("/{eventId}")
	public ResponseEntity<ResponseDto> deleteEvent(@PathVariable Long eventId) {
		eventService.deleteEvent(eventId);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글이 삭제 되었습니다.")
			.build()
		);
	}

	// @GetMapping("/{eventId}")
	// public EventResponseDto detail(@PathVariable("eventId") Long eventId) {
	// 	return eventService.getEvent(eventId);
	// }

	// 전체 글 보기 페이지(홈) -> 일단 테스트용으로 올린거 전부
	@GetMapping("/all")
	public Slice<EventResponseDto> getEventList(Pageable pageable) {
		return eventService.getEventList(pageable);
	}


	// type별로 글 보기
	@GetMapping("/{type}")
	public Slice<EventResponseDto> getEventTypeList(@PathVariable String type, @PageableDefault(size = 10) Pageable pageable) {
		return eventService.getEventTypeList(type, pageable);
	}

	@GetMapping("/test")
	public String testGetMethod() {
		return "get";
	}
}
