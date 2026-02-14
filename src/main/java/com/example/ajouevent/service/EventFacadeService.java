package com.example.ajouevent.service;

import java.security.Principal;

import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import com.example.ajouevent.dto.EventDetailResponseDto;
import com.example.ajouevent.dto.EventResponseDto;
import com.example.ajouevent.dto.EventWithKeywordDto;
import com.example.ajouevent.dto.SliceResponse;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.util.SecurityUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventFacadeService {
	private final EventQueryService eventQueryService;
	private final EventViewService eventViewService;
	private final TopicService topicService;
	private final KeywordService keywordService;

	// 이벤트 상세 조회 및 조회수 증가
	public EventDetailResponseDto getEventDetail(Long eventId, Principal principal, HttpServletRequest request, HttpServletResponse response) {
		String userId = SecurityUtil.getCurrentMemberUsernameOrAnonymous();

		if (isAnonymous(userId)) {
			eventViewService.handleAnonymousUser(request, response, eventId);
		} else {
			eventViewService.handleAuthenticatedUser(userId, eventId);
		}

		return eventQueryService.getEventDetail(eventId, principal);
	}

	private boolean isAnonymous(String userId) {
		return "Anonymous".equals(userId);
	}

	// 토픽 기반 글 조회
	public SliceResponse<EventResponseDto> getEventTypeList(String type, String keyword, Pageable pageable, Principal principal) {
		if (principal != null) {
			topicService.markTopicAsRead(type, principal.getName());
		}

		return eventQueryService.getEventTypeList(type, keyword, pageable, principal);
	}

	// 키워드 기반 글 조회
	public SliceResponse<EventWithKeywordDto> getClubEventsByKeyword(String searchKeyword, Principal principal, Pageable pageable) {
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		keywordService.markKeywordAsRead(searchKeyword, principal.getName());

		return eventQueryService.getClubEventsByKeyword(searchKeyword, principal, pageable);
	}


}