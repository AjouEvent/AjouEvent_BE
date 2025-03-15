package com.example.ajouevent.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.dto.BannerDto;
import com.example.ajouevent.dto.BannerRequest;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.service.BannerService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/banners")
public class BannerController {
	private final BannerService bannerService;

	@PreAuthorize("hasRole('ADMIN')")
	@PostMapping("")
	public ResponseEntity<ResponseDto> addBanner(@RequestBody BannerRequest request) {
		BannerDto bannerDto = bannerService.addBanner(request);
		return ResponseEntity.ok(ResponseDto.builder()
			.successStatus(HttpStatus.CREATED)
			.successContent("배너가 추가되었습니다.")
			.Data(bannerDto)
			.build());
	}

	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{bannerId}")
	public ResponseEntity<ResponseDto> deleteBanner(@PathVariable("bannerId") Long bannerId) {
		bannerService.deleteBanner(bannerId);
		return ResponseEntity.ok(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("배너가 삭제되었습니다.")
			.build());
	}

	@GetMapping("")
	public List<BannerDto> getAllBanners() {
		return bannerService.getAllBanners();
	}
}