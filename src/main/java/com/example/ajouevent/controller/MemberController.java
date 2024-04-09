package com.example.ajouevent.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.dto.RegisterRequest;
import com.example.ajouevent.dto.ResponseDTO;
import com.example.ajouevent.dto.MemberDTO;
import com.example.ajouevent.dto.MemberResponse;
import com.example.ajouevent.service.FCMService;
import com.example.ajouevent.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
public class MemberController {

	private final FCMService fcmService;
	private final MemberService MemberService;

	@PostMapping("/register")
	public String register(@RequestBody RegisterRequest request) {
		MemberService.register(request);
		return "가입 완료";
	}

	@PostMapping("/login")
	public ResponseEntity<ResponseDTO> login(@RequestBody MemberDTO.LoginRequest loginRequest){
		// MemberService.login(loginRequest);
		fcmService.saveToken(loginRequest);
		// TokenDto tokenDto = loginService.login(dto.getMemberName(), dto.getPassword());
		return ResponseEntity.ok().body(ResponseDTO.builder()
			.successStatus(HttpStatus.OK)
			.successContent(loginRequest.getEmail()+"님이 로그인 되었습니다. 환영합니다.")
			.Data(loginRequest)
			.build()
		);
	}
}
