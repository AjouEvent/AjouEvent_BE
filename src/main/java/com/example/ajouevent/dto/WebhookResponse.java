package com.example.ajouevent.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class WebhookResponse {

	private String result;
	private String topic;

	@Builder
	public WebhookResponse(String result, String topic) {
		this.result = result;
		this.topic = topic;
	}
}