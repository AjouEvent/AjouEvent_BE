package com.example.ajouevent.controller;

import com.example.ajouevent.dto.*;
import com.example.ajouevent.dto.ReissueTokenDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.example.ajouevent.dto.RegisterRequest;
import com.example.ajouevent.dto.MemberDto;
import com.example.ajouevent.service.FCMService;
import com.example.ajouevent.service.MemberService;
import com.example.ajouevent.service.TopicService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class MemberController {

	private final FCMService fcmService;
	private final MemberService memberService;
	private final TopicService topicService;

	@PostMapping("/register")
	public String register(@RequestBody RegisterRequest request) throws IOException {
		return memberService.register(request);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody MemberDto.LoginRequest loginRequest){
		log.info("/login api 호출");

		topicService.saveFCMToken(loginRequest);
		return memberService.login(loginRequest);
	}

	@PatchMapping("/reissue-token")
	public ResponseEntity<LoginResponse> reissueAccessToken(@RequestBody ReissueTokenDto refreshToken) {
		LoginResponse token = memberService.reissueAccessToken(refreshToken);
		return ResponseEntity.status(HttpStatus.OK).body(token);
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("")
	public ResponseEntity<MemberGetDto> getMemberInfo(Principal principal) {
		MemberGetDto memberGetDtoList = memberService.getMemberInfo(principal);
		return ResponseEntity.status(HttpStatus.OK).body(memberGetDtoList);
	}

}
