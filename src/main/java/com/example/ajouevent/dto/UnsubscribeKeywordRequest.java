package com.example.ajouevent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnsubscribeKeywordRequest {
	private String englishKeyword;

	@JsonCreator
	public UnsubscribeKeywordRequest(String englishKeyword) {
		this.englishKeyword = englishKeyword;
	}
}
