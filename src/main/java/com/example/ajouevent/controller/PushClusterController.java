package com.example.ajouevent.controller;

import java.util.List;

import com.example.ajouevent.dto.PushClusterStatsResponse;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.service.PushClusterService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/push-cluster")
public class PushClusterController {

	private final PushClusterService pushClusterService;

	@GetMapping("")
	public List<PushClusterStatsResponse> getPushClusterStats() {
		return pushClusterService.calculateAllPushClusterStats();
	}

	@PostMapping("/received")
	public ResponseEntity<ResponseDto> incrementReceived(@RequestBody Long pushClusterId) {
		pushClusterService.incrementReceived(pushClusterId);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("Push received count incremented.")
			.Data(pushClusterId)
			.build()
		);
	}

	@PostMapping("/clicked")
	public ResponseEntity<ResponseDto> incrementClicked(@RequestBody Long pushClusterId) {
		pushClusterService.incrementClicked(pushClusterId);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("Push clicked count incremented.")
			.Data(pushClusterId)
			.build()
		);
	}
}