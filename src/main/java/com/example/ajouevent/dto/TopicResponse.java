package com.example.ajouevent.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Getter;

@Getter
public class TopicResponse {
	private final List<String> topics;

	@JsonCreator
	public TopicResponse(List<String> topics) {
		this.topics = topics;
	}
}
