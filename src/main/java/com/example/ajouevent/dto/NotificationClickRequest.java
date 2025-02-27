package com.example.ajouevent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class NotificationClickRequest {

	private Long pushNotificationId;

	@JsonCreator
	public NotificationClickRequest(Long pushNotificationId) {
		this.pushNotificationId = pushNotificationId;
	}
}