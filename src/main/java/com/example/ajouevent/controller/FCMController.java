package com.example.ajouevent.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.example.ajouevent.dto.UserDTO;
import com.example.ajouevent.service.FCMService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class FCMController {
	private final FCMService fcmService;

	//클라이언트에게 FCM registraion을 받아 user_id값과 매필하여 DB에 저장하기
	@PostMapping("/send/registeration-token")
	public void saveClientId(@RequestBody UserDTO.LoginRequest loginRequest){
		fcmService.saveClientId(loginRequest);
	}
}
