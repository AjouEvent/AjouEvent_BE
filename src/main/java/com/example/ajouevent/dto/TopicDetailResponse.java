package com.example.ajouevent.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopicDetailResponse {
	private final String classification;
	private final Long koreanOrder;
	private final String koreanTopic;
}