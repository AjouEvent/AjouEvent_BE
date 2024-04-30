package com.example.ajouevent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.ajouevent.dto.MemberDto;
import com.example.ajouevent.service.FCMService;
import com.example.ajouevent.service.TopicService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FCMController {
	private final FCMService fcmService;
	private final TopicService topicService;

	//클라이언트에게 FCM registraion을 받아 Member_id값과 매필하여 DB에 저장하기
	@PostMapping("/send/registeration-token")
	public void saveClientId(@RequestBody MemberDto.LoginRequest loginRequest){
		log.info("/send/registeration-token 호출");
		topicService.saveFCMToken(loginRequest);
	}
}
