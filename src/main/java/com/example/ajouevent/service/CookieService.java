package com.example.ajouevent.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import com.example.ajouevent.domain.ClubEvent;

@Component
public class CookieService {

	private final static String VIEWCOOKIENAME = "AlreadyView";

	public ResponseCookie createOrUpdateCookie(String currentCookieValue, Long postId) {
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

		return ResponseCookie.from(VIEWCOOKIENAME, updatedValue)
			.path("/")
			.sameSite("None")
			.httpOnly(true)
			.secure(true) // secure 옵션을 true로 변경한다.
            .maxAge(24 * 60 * 60)
			.build();
	}

	public boolean isPostViewed(String currentCookieValue, Long eventId) {
		if (currentCookieValue == null) return false;
		Set<Long> viewedPostIds = Stream.of(currentCookieValue.split("/"))
			.map(Long::parseLong)
			.collect(Collectors.toSet());
		return viewedPostIds.contains(eventId);
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