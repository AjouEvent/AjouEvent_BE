package com.example.ajouevent.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.example.ajouevent.domain.ClubEvent;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CookieService {

	public static final String VIEW_COOKIE_NAME = "AlreadyView";
	private static final int COOKIE_MAX_AGE_SECONDS = 24 * 60 * 60; // 1일 (24시간)

	public ResponseCookie createOrUpdateCookie(String currentCookieValue, ClubEvent clubEvent) {
		Set<Long> viewedEventIds = new HashSet<>();
		if (StringUtils.hasText(currentCookieValue)) {
			viewedEventIds = Stream.of(currentCookieValue.split("/"))
				.map(Long::parseLong)
				.collect(Collectors.toSet());
		}
		viewedEventIds.add(clubEvent.getEventId());
		String updatedValue = viewedEventIds.stream()
			.map(String::valueOf)
			.collect(Collectors.joining("/"));

		return ResponseCookie.from(getCookieName(clubEvent), updatedValue)
			.path("/")
			.sameSite("None")
			.httpOnly(true)
			.secure(true) // secure 옵션을 true로 변경한다.
            .maxAge(getExpirationInSeconds(COOKIE_MAX_AGE_SECONDS))
			.build();
	}

	public boolean isAlreadyViewed(String currentCookieValue, Long eventId) {
		if (!StringUtils.hasText(currentCookieValue)) return false;
		Set<Long> viewedPostIds = Stream.of(currentCookieValue.split("/"))
			.map(Long::parseLong)
			.collect(Collectors.toSet());
		return viewedPostIds.contains(eventId);
	}

	public String getCookieValue(HttpServletRequest request, ClubEvent clubEvent) {
		Cookie[] cookies = request.getCookies();
		log.info("기존 쿠키" + Arrays.toString(cookies));
		if (cookies != null) {
			return Arrays.stream(cookies)
				.filter(cookie -> getCookieName(clubEvent).equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElse("");
		}
		return "";
	}


	public int getExpirationInSeconds(int expirationInSeconds) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expirationTime = now.plusSeconds(expirationInSeconds);
		return (int) now.until(expirationTime, ChronoUnit.SECONDS);
	}

	public String getCookieName(Object reference) {
		return VIEW_COOKIE_NAME + getObjectName(reference);
	}

	public String getObjectName(Object reference) {
		String objectType;
		if (reference instanceof ClubEvent) {
			objectType = "ClubEventNum";
		} else {
			objectType = "UnknownNum";
		}
		return objectType;
	}
}