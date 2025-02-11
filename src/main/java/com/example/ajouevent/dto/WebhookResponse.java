package com.example.ajouevent.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WebhookResponse {

	private String result;
	private String topic;
	private String title;
	private Long eventId;

	@Builder
	public WebhookResponse(String result, String topic, String title, Long eventId) {
		this.result = result;
		this.topic = topic;
		this.title = title;
		this.eventId = eventId;
	}
}