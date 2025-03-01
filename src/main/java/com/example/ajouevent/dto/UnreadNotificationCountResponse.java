package com.example.ajouevent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class UnreadNotificationCountResponse {
	private int unreadNotificationCount;

	@JsonCreator
	public UnreadNotificationCountResponse(int unreadNotificationCountCount) {
		this.unreadNotificationCount = unreadNotificationCountCount;
	}
}