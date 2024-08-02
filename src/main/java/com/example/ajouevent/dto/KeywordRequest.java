package com.example.ajouevent.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class KeywordRequest {
	private String englishKeyword;
	private String koreanKeyword;
	private String topicName;
}
