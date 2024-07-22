package com.example.ajouevent.controller;

import java.security.Principal;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.dto.PostNotificationDto;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.service.AlarmService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/alarm")
public class AlarmController {
	private final AlarmService alarmService;

	// 알람 등록
	@PreAuthorize("isAuthenticated()")
	@PostMapping("")
	public ResponseEntity<ResponseDto> createAlarm(@RequestBody PostNotificationDto postNotificationDTO, Principal principal) {
		alarmService.createAlarm(postNotificationDTO, principal);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(postNotificationDTO.getAlarmDateTime() +" 에 알람 전송을 합니다.")
			.build()
		);
	}
}
