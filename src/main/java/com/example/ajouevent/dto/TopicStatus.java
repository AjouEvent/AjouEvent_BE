package com.example.ajouevent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class TopicStatus {
	private final Long id;
	private final String koreanTopic;
	private final String englishTopic;
	private final String classification;
	private final boolean subscribed;
	private final Long koreanOrder;
}