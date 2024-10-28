package com.example.ajouevent.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class SecurityUtil {

	public static String getCurrentMemberUsernameOrAnonymous() {
		final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || authentication.getName() == null || authentication.getName().equals("anonymousUser")) {
			return "Anonymous";
		}

		return authentication.getName();
	}
}
