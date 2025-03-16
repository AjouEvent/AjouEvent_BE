package com.example.ajouevent.service;

import java.security.Principal;

import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.dto.EventDetailResponseDto;
import com.example.ajouevent.dto.EventResponseDto;
import com.example.ajouevent.dto.SliceResponse;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.EventRepository;
import com.example.ajouevent.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventFacadeService {
	private final EventQueryService eventQueryService;
	private final EventCommandService eventCommandService;
	private final EventRepository eventRepository;

	// 이벤트 상세 조회 및 조회수 증가
	public EventDetailResponseDto getEventDetail(Long eventId, Principal principal, HttpServletRequest request, HttpServletResponse response) {
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		String userId = SecurityUtil.getCurrentMemberUsernameOrAnonymous();

		if (isAnonymous(userId)) {
			eventCommandService.handleAnonymousUserWithCookieAndRedis(request, response, clubEvent);
		} else {
			eventCommandService.handleAuthenticatedUser(userId, clubEvent);
		}

		return eventQueryService.getEventDetail(eventId, principal);
	}

	// 글 타입별 조회 (동아리, 학생회, 공지사항, 기타)
	public SliceResponse<EventResponseDto> getEventTypeList(String type, String keyword, Pageable pageable, Principal principal) {
		if (principal != null) {
			eventCommandService.markTopicAsRead(type, principal.getName());
		}

		return eventQueryService.getEventTypeList(type, keyword, pageable, principal);
	}

	private boolean isAnonymous(String userId) {
		return "Anonymous".equals(userId);
	}
}