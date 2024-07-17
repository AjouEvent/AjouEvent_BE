package com.example.ajouevent.util;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;
import com.example.ajouevent.domain.ClubEvent;
import jakarta.servlet.http.Cookie;

@Component
public class CookieUtil {

	private final static String VIEWCOOKIENAME = "AlreadyView";

	public Cookie createOrUpdateCookie(String currentCookieValue, Long postId) {
		Set<Long> viewedPostIds = new HashSet<>();
		if (currentCookieValue != null) {
			viewedPostIds = Stream.of(currentCookieValue.split("/"))
				.map(Long::parseLong)
				.collect(Collectors.toSet());
		}
		viewedPostIds.add(postId);
		String updatedValue = viewedPostIds.stream()
			.map(String::valueOf)
			.collect(Collectors.joining("/"));
		Cookie cookie = new Cookie(VIEWCOOKIENAME, updatedValue);
		cookie.setMaxAge(24 * 60 * 60); // 24 hours
		cookie.setHttpOnly(true); // Server-side only
		cookie.setPath("/");
		return cookie;
	}

	public boolean isPostViewed(String currentCookieValue, Long postId) {
		if (currentCookieValue == null) return false;
		Set<Long> viewedPostIds = Stream.of(currentCookieValue.split("/"))
			.map(Long::parseLong)
			.collect(Collectors.toSet());
		return viewedPostIds.contains(postId);
	}


	public int getExpirationInSeconds(int expirationInSeconds) {
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime expirationTime = now.plusSeconds(expirationInSeconds);
		return (int) now.until(expirationTime, ChronoUnit.SECONDS);
	}

	public String getCookieName(Long postId, Object reference) {
		return VIEWCOOKIENAME + getObjectName(reference) + "-No." + postId;
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