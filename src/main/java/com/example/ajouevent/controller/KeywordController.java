package com.example.ajouevent.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.dto.KeywordRequest;
import com.example.ajouevent.dto.KeywordResponse;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.dto.UnsubscribeKeywordRequest;
import com.example.ajouevent.service.KeywordService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/keyword")
@RequiredArgsConstructor
public class KeywordController {

	private final KeywordService keywordService;

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/subscribe")
	public ResponseEntity<ResponseDto> subscribeToKeyword(@RequestBody KeywordRequest keywordRequest) {
		keywordService.subscribeToKeyword(keywordRequest);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(keywordRequest.getKoreanKeyword() +" 키워드를 구독합니다.")
			.build()
		);
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/unsubscribe")
	public ResponseEntity<ResponseDto> unsubscribeFromKeyword(@RequestBody UnsubscribeKeywordRequest unsubscribeKeywordRequest) {
		keywordService.unsubscribeFromKeyword(unsubscribeKeywordRequest);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent(unsubscribeKeywordRequest.getEnglishKeyword() +" 키워드를 구독 취소합니다.")
			.build()
		);
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/userKeywords")
	public List<KeywordResponse> getUserKeywordNotifications(Principal principal) {
		return keywordService.getUserKeyword(principal);
	}

	@PreAuthorize("isAuthenticated()")
	@DeleteMapping("/subscriptions/reset")
	public ResponseEntity<ResponseDto> resetSubscriptions() {
		keywordService.resetAllSubscriptions();
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("사용자의 모든 keyword 구독을 초기화합니다.")
			.build()
		);
	}

}
