package com.example.ajouevent.controller;

import com.example.ajouevent.dto.*;
import com.example.ajouevent.dto.ReissueTokenDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.ajouevent.dto.RegisterRequest;
import com.example.ajouevent.dto.MemberDTO;
import com.example.ajouevent.service.FCMService;
import com.example.ajouevent.service.MemberService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.security.Principal;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
public class MemberController {

	private final FCMService fcmService;
	private final MemberService memberService;

	@PostMapping("/register")
	public String register(@RequestBody RegisterRequest request) throws IOException {
		return memberService.register(request);
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody MemberDTO.LoginRequest loginRequest){
		fcmService.saveToken(loginRequest);
		return memberService.login(loginRequest);
	}

	@PatchMapping("/reissue-token")
	public ResponseEntity<LoginResponse>  reissueAccessToken(@RequestBody ReissueTokenDto refreshToken) {
		LoginResponse token = memberService.reissueAccessToken(refreshToken);
		return ResponseEntity.status(HttpStatus.OK).body(token);
	}

	@SecurityRequirement(name = "access-token")
	@PostMapping("/test")
	public String test(Principal principal) {
		System.out.println(principal.getName());
		return "success";
	}

}
