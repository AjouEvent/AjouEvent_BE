package com.example.ajouevent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class TopicRequest {
	private String topic;

	@JsonCreator
	public TopicRequest(String topic) {
		this.topic = topic;
	}
}
