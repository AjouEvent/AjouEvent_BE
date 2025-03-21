package com.example.ajouevent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.ajouevent.dto.MemberDto;
import com.example.ajouevent.service.TokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequiredArgsConstructor
@Slf4j
public class FCMController {
	private final TokenService tokenService;

	//클라이언트에게 FCM registraion을 받아 Member_id값과 매필하여 DB에 저장하기
	@PostMapping("/send/registeration-token")
	public void saveClientId(@RequestBody MemberDto.LoginRequest loginRequest){
		log.info("/send/registeration-token 호출");
		tokenService.registerTokenWithSubscriptions(loginRequest);
	}
}
