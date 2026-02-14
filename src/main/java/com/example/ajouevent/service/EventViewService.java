package com.example.ajouevent.service;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.EventRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventViewService {

	private final CookieService cookieService;
	private final RedisService redisService;
	private final StringRedisTemplate stringRedisTemplate;
	private final EventRepository eventRepository;

	// 익명 사용자 처리
	public void handleAnonymousUser(HttpServletRequest request, HttpServletResponse response, Long eventId) {
		ClubEvent clubEvent = getClubEvent(eventId);

		String ipAddress = getClientIp(request);
		String userAgent = request.getHeader("User-Agent");
		String cookieValue = cookieService.getCookieValue(request, clubEvent);

		// Redis 키 생성
		String redisKey = "ClubEvent_View:" + clubEvent.getEventId() + ":" + ipAddress + ":" + userAgent;

		// 쿠키가 없거나 조회된 적 없는 경우
		if (!cookieService.isAlreadyViewed(cookieValue, clubEvent.getEventId())) {
			ResponseCookie newCookie = cookieService.createOrUpdateCookie(cookieValue, clubEvent);
			response.addHeader("Set-Cookie", newCookie.toString());

			// 쿠키가 없으면 Redis에서 한 번 더 확인
			if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(redisKey))) {
				stringRedisTemplate.opsForValue().set(redisKey, "0", 86400L, TimeUnit.SECONDS); // TTL과 함께 설정

				// 조회수 증가
				increaseViews(clubEvent);
			}
		}
	}

	// 로그인 사용자 처리
	public void handleAuthenticatedUser(String userId, Long eventId) {
		ClubEvent clubEvent = getClubEvent(eventId);
		if (redisService.isFirstIpRequest(userId, clubEvent.getEventId(), clubEvent)) {
			redisService.writeClientRequest(userId, clubEvent.getEventId(), clubEvent);
			increaseViews(clubEvent);
		}
	}

	// 조회수 증가
	public void increaseViews(ClubEvent clubEvent){
		String key = "ClubEvent:views:" + clubEvent.getEventId();
		Boolean exist = stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(clubEvent.getViewCount()+1),4L, TimeUnit.MINUTES);
		if(Boolean.FALSE.equals(exist)){
			stringRedisTemplate.opsForValue().increment(key);
			stringRedisTemplate.expire(key,4L,TimeUnit.MINUTES);
		}
	}

	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip.split(",")[0].trim(); // X-Forwarded-For는 콤마로 구분된 여러 IP를 가질 수 있음
	}

	private ClubEvent getClubEvent(Long eventId) {
		return eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));
	}
}