package com.example.ajouevent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnsubscribeKeywordRequest {
	private String encodedKeyword;

	@JsonCreator
	public UnsubscribeKeywordRequest(String encodedKeyword) {
		this.encodedKeyword = encodedKeyword;
	}
}
