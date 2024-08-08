package com.example.ajouevent.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopicResponse {
	private final Long id;
	private final String koreanTopic;
	private final String englishTopic;
}
