package com.example.ajouevent.dto;

import java.util.List;

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
	// private List<String> topics;
	private String topic;

	// @JsonCreator
	// public TopicRequest(List<String> topics) {
	// 	this.topics = topics;
	// }

	@JsonCreator
	public TopicRequest(String topic) {
		this.topic = topic;
	}

}
