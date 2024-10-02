package com.example.ajouevent.controller;

import com.example.ajouevent.auth.*;
import com.example.ajouevent.dto.*;
import com.example.ajouevent.dto.ReissueTokenDto;
import com.google.api.Http;
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

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Slf4j
public class MemberController {

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

	// 사용자 등록 API
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/register-info")
	public ResponseEntity<RegisterResponse> registerInfo(@RequestBody RegisterMemberInfoRequest registerMemberInfoRequest, Principal principal) throws IOException {
		RegisterResponse responseMessage = memberService.registerInfo(registerMemberInfoRequest, principal);
		return ResponseEntity.status(HttpStatus.CREATED).body(responseMessage);
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

	@PreAuthorize("isAuthenticated()")
	@PatchMapping("")
	public ResponseEntity<String> updateMemberInfo (@RequestBody MemberUpdateDto memberUpdateDto, Principal principal) {
		String res = memberService.updateMemberInfo(memberUpdateDto, principal);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

	@PreAuthorize("isAuthenticated()")
	@DeleteMapping("")
	public ResponseEntity<String> deleteMember (Principal principal) {
		String res = memberService.deleteMember(principal);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

	@PostMapping("/oauth")
	public ResponseEntity<LoginResponse> getAccessToken (@RequestBody OAuthDto oAuthDto) throws GeneralSecurityException, IOException {
		LoginResponse loginResponse = memberService.socialLogin(oAuthDto);
		return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
	}

	@GetMapping("/duplicateEmail")
	public ResponseEntity<Boolean> duplicateEmail (@RequestParam(name="email") String email) {
		Boolean res = memberService.duplicateEmail(email);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

	@GetMapping("/emailExists")
	public ResponseEntity<Boolean> emailExists (@RequestParam(name="email") String email) {
		Boolean res = memberService.emailExists(email);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

	@GetMapping("/accountExists")
	public ResponseEntity<Boolean> accountExists (@RequestParam(name="email") String email, @RequestParam(name="name") String name) {
		Boolean res = memberService.accountExists(email, name);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

	@PostMapping("/verify-current-password")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<Boolean> verifyCurrentPassword(@RequestBody CurrentPasswordDto currentPasswordDto, Principal principal) {
		boolean isPasswordValid = memberService.verifyCurrentPassword(currentPasswordDto, principal);
		return ResponseEntity.status(HttpStatus.OK).body(isPasswordValid);
	}

	@PostMapping("/emailCheckRequest")
	public ResponseEntity<String> emailCheckRequest (@RequestParam(name="email") String email) {
		String res = memberService.EmailCheckRequest(email);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

	@PostMapping("/emailCheck")
	public ResponseEntity<String> emailCheck (@RequestParam(name="email") String email, @RequestParam(name="code") String code) {
		String res = memberService.EmailCheck(email, code);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

	// 캘린더 연동
	@PostMapping("/calendar")
	public ResponseEntity<String> ConnectCalendar (@RequestBody OAuthDto oAuthDto) throws GeneralSecurityException, IOException {
		UserInfoGetDto userInfoGetDto = memberService.connectCalendar(oAuthDto);
		return ResponseEntity.status(HttpStatus.OK).body(userInfoGetDto.getEmail());
	}


	@PatchMapping("/change-password")
	@PreAuthorize("isAuthenticated()")
	public ResponseEntity<String> changePassword (@RequestBody PasswordDto passwordDto, Principal principal) {
		String res = memberService.changePassword(passwordDto, principal);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

	@PatchMapping("/reset-password")
	public ResponseEntity<String> changePassword (@RequestBody ResetPasswordDto resetPasswordDto) {
		String res = memberService.resetPassword(resetPasswordDto);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

	// 비밀번호 재발급
	@PostMapping("/reissue-password")
	public ResponseEntity<String> reissuePassword (@RequestBody ReissuePasswordDto reissuePasswordDto) throws Exception {
		String res = memberService.reissuePassword(reissuePasswordDto);
		return ResponseEntity.status(HttpStatus.OK).body(res);
	}

}
