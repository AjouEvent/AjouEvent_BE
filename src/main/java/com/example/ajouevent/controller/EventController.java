package com.example.ajouevent.controller;

import static org.springframework.data.domain.Sort.Direction.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.List;

import com.example.ajouevent.dto.*;
import com.example.ajouevent.service.CalendarService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.ajouevent.dto.EventDetailResponseDto;
import com.example.ajouevent.dto.EventResponseDto;
import com.example.ajouevent.dto.PostEventDto;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.dto.SliceResponse;
import com.example.ajouevent.dto.UpdateEventRequest;
import com.example.ajouevent.service.EventService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/event")
public class EventController {

	private final EventService eventService;
	private final CalendarService calendarService;

	// 게시글 생성 - S3 스프링부트에서 변환
	@PostMapping("/new")
	public ResponseEntity<ResponseDto> newEvent(@Valid @RequestPart(value = "data") PostEventDto postEventDto,
		@RequestPart(value = "image", required = false) List<MultipartFile> images) throws IOException {
		eventService.newEvent(postEventDto, images);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글이 성공적으로 업로드되었습니다.")
			.build()
		);
	}

	// 게시글 생성 - S3 프론트에서 변환
	@PostMapping("/post")
	public ResponseEntity<ResponseDto> postEvent(@Valid @RequestBody PostEventDto postEventDto) {
		eventService.postEvent(postEventDto);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글이 성공적으로 업로드되었습니다.")
			.build()
		);
	}

	// 게시글 수정 - 데이터
	@PatchMapping("/{eventId}")
	public ResponseEntity<ResponseDto> updateEventData(@PathVariable("eventId") Long eventId,
		@RequestBody UpdateEventRequest request) {
		eventService.updateEventData(eventId, request);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글 데이터가 수정되었습니다.")
			.build()
		);
	}

	// 게시글 수정 - 이미지
	@PatchMapping("/{eventId}/images")
	public ResponseEntity<ResponseDto> updateEventImages(@PathVariable("eventId") Long eventId,
		@RequestPart("image") List<MultipartFile> images) throws IOException {
		eventService.updateEventImages(eventId, images);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글 이미지가 수정되었습니다.")
			.build());
	}


	// 게시글 삭제
	@PreAuthorize("hasRole('ADMIN')")
	@DeleteMapping("/{eventId}")
	public ResponseEntity<ResponseDto> deleteEvent(@PathVariable("eventId") Long eventId) {
		eventService.deleteEvent(eventId);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글이 삭제 되었습니다.")
			.build()
		);
	}

	// 게시글 상세 조회
	@PreAuthorize("permitAll()")
	@GetMapping("/detail/{eventId}")
	public EventDetailResponseDto detail(@PathVariable("eventId") Long eventId, Principal principal, HttpServletRequest request, HttpServletResponse response) {
		return eventService.getEventDetail(eventId, principal, request, response);
	}

	// 전체 글 보기 페이지(홈) -> 일단 테스트용으로 올린거 전부
	@PreAuthorize("permitAll()")
	@GetMapping("/all")
	public SliceResponse<EventResponseDto> getEventList(@RequestParam(required = false, defaultValue = "", name="keyword") String keyword, @PageableDefault(sort = "createdAt", direction = DESC) Pageable pageable, Principal principal) {
		return eventService.getEventList(pageable, keyword, principal);
	}

	// type별로 글 보기
	@PreAuthorize("permitAll()")
	@GetMapping("/{type}")
	public SliceResponse<EventResponseDto> getEventTypeList(@PathVariable("type") String type, @RequestParam(required = false, defaultValue = "", name="keyword") String keyword, @PageableDefault(sort = "createdAt", direction = DESC) Pageable pageable, Principal principal) {
		return eventService.getEventTypeList(type, keyword, pageable, principal);
	}

	// 인기글 조회 로직
	@PreAuthorize("permitAll()")
	@GetMapping("/popular")
	public List<EventResponseDto> getPopularEvents(Principal principal) {
		return eventService.getTopPopularEvents(principal);
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/subscribed")
	public SliceResponse<EventResponseDto> getSubscribedEvent(
		@PageableDefault(sort = "createdAt", direction = DESC) Pageable pageable,
		Principal principal, @RequestParam(value = "keyword", required = false) String keyword) {
		return eventService.getSubscribedEvents(pageable, principal, keyword);
	}

	// 게시글 찜하기
	@PreAuthorize("isAuthenticated()")
	@PostMapping("/like/{eventId}")
	public ResponseEntity<ResponseDto> likeEvent(@PathVariable("eventId") Long eventId, Principal principal) {
		return eventService.likeEvent(eventId, principal);
	}

	// 게시글 찜 취소
	@PreAuthorize("isAuthenticated()")
	@DeleteMapping("/like/{eventId}")
	public ResponseEntity<ResponseDto> cancelLikeEvent(@PathVariable("eventId") Long eventId, Principal principal) {
		return eventService.cancelLikeEvent(eventId, principal);
	}

	// 찜한 게시글 불러오기
	@PreAuthorize("isAuthenticated()")
	@GetMapping("/liked")
	public SliceResponse<EventResponseDto> getLikedEvents(@RequestParam(required = false, name = "type") String type,
		@RequestParam(required = false, defaultValue = "", name="keyword") String keyword,
		@PageableDefault(sort = "createdAt", direction = DESC) Pageable pageable, Principal principal) {
		return eventService.getLikedEvents(type, keyword, pageable, principal);
	}

	// 홈화면에 들어갈 이벤트 배너 추가 API
	@PreAuthorize("hasRole('ADMIN')")  // ADMIN 권한만 접근 가능
	@PostMapping("/addBanner")
	public ResponseEntity<ResponseDto> addEventBanner(@RequestBody EventBannerRequest eventBannerRequest) {
		eventService.addEventBanner(eventBannerRequest);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("이벤트 배너에 등록되었습니다.")
			.build()
		);
	}

	// 이벤트 배너 삭제 API
	@PreAuthorize("hasRole('ADMIN')")  // ADMIN 권한만 접근 가능
	@DeleteMapping("/deleteBanner/{eventBannerId}")
	public ResponseEntity<ResponseDto> deleteEventBanner(@PathVariable("eventBannerId") Long eventBannerId) {
		eventService.deleteEventBanner(eventBannerId);
		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("이벤트 배너가 삭제되었습니다.")
			.build()
		);
	}

	// 홈화면에 들어갈 이벤트 배너 불러오기
	@PreAuthorize("permitAll()")
	@GetMapping("/banner")
	public List<EventBannerDto> getAllEventBanners() {
		return eventService.getAllEventBanners();
	}

	@PreAuthorize("isAuthenticated()")
	@PostMapping("/calendar")
	public void testGetMethod(@RequestBody CalendarStoreDto calendarStoreDto, Principal principal) throws GeneralSecurityException, IOException {
		calendarService.GoogleAPIClient(calendarStoreDto, principal);
    }

	@GetMapping("/test")
	public String testGetMethod() {
		return "get";
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/getSubscribedPostsByKeyword")
	public SliceResponse<EventWithKeywordDto> getAllClubEventsBySubscribedKeywords(Principal principal,
		@PageableDefault(size = 10, sort = "createdAt", direction = DESC) Pageable pageable) {
		return eventService.getAllClubEventsBySubscribedKeywords(principal, pageable);
	}

	@PreAuthorize("isAuthenticated()")
	@GetMapping("/getSubscribedPostsByKeyword/{keyword}")
	public SliceResponse<EventWithKeywordDto> getClubEventsByKeyword(@PathVariable("keyword") String searchKeyword,
		Principal principal,
		@PageableDefault(size = 10, sort = "createdAt", direction = DESC) Pageable pageable) {
		return eventService.getClubEventsByKeyword(searchKeyword, principal, pageable);
	}
}
