package com.example.ajouevent.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WebhookResponse {

	private String result;
	private String topic;
	private Long eventId;

	@Builder
	public WebhookResponse(String result, String topic, Long eventId) {
		this.result = result;
		this.topic = topic;
		this.eventId = eventId;
	}
}