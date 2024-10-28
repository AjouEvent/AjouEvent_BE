package com.example.ajouevent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeywordResponse {
	private String encodedKeyword;
	private String koreanKeyword;
	private String searchKeyword;
	private String topicName;
}