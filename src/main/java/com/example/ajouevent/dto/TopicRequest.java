package com.example.ajouevent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicRequest {
	private String topic;
	@JsonCreator
	public TopicRequest(String topic) {
		this.topic = topic;
	}

}
