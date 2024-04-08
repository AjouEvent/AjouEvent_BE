package com.example.ajouevent.controller;

import com.example.ajouevent.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.example.ajouevent.service.FCMService;
import com.example.ajouevent.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
@Slf4j
public class UserController {

	private final FCMService fcmService;
	private final UserService userService;

	@PostMapping("/register")
	public String register(@RequestBody RegisterRequest request) throws IOException {
		userService.register(request);
		return "가입 완료";
	}

	@PostMapping("/login")
	public ResponseEntity<LoginResponse> login(@RequestBody UserDTO.LoginRequest loginRequest){
		fcmService.saveToken(loginRequest);
		return userService.login(loginRequest);
	}

	@PatchMapping("/reissue-token")
	public ResponseEntity<LoginResponse>  reissueAccessToken(@RequestBody ReissueTokenDto refreshToken) {
		LoginResponse token = this.userService.reissueAccessToken(refreshToken);
		return ResponseEntity.status(HttpStatus.OK).body(token);
	}
}
