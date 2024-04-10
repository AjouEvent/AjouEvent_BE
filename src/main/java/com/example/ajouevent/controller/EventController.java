package com.example.ajouevent.controller;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.example.ajouevent.dto.MemberDTO;
import com.example.ajouevent.dto.PostEventDTO;
import com.example.ajouevent.dto.PostNotificationDTO;
import com.example.ajouevent.dto.ResponseDTO;
import com.example.ajouevent.service.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/event")
public class EventController {

	private final EventService eventService;

	// @PostMapping("/send")
	// public void startEventTime(@RequestBody MemberDTO.LoginRequest loginRequest){
	// 	eventService.sendEventNotification();
	// }

	// 알림 등록 - 동아리, 학생회 이벤트 + 공지사항 크롤링
	@PostMapping("/notification")
	public ResponseEntity<ResponseDTO> postNotification(@RequestBody PostNotificationDTO postNotificationDTO, Principal principal) {
		eventService.createNotification(postNotificationDTO, principal);
		return ResponseEntity.ok().body(ResponseDTO.builder()
			.successStatus(HttpStatus.OK)
			.successContent(postNotificationDTO.getAlarmDateTime() +" 에 알림 전송을 합니다.")
			.build()
		);
	}

	// 게시글 생성
	@PostMapping("/new")
	public ResponseEntity<ResponseDTO> postEvent(@RequestPart(value = "data") PostEventDTO postEventDto, @RequestPart(value = "image", required = false)
	List<MultipartFile> images) throws IOException {
		eventService.postEvent(postEventDto, images);
		return ResponseEntity.ok().body(ResponseDTO.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글이 성공적으로 업로드되었습니다.")
			.build()
		);
	}

	@GetMapping("/test")
	public String testGetMethod() {
		return "get";
	}

//	@PostMapping("/new")
//	public void createEvent(@RequestPart MultipartFile image, @RequestPart PostEventDto postEventDto){
//
//	}
}
