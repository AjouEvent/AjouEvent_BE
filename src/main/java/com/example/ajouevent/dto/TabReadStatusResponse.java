package com.example.ajouevent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class TabReadStatusResponse {
	private boolean isSubscribedTabRead;

	@JsonCreator
	public TabReadStatusResponse(boolean isSubscribedTabRead) {
		this.isSubscribedTabRead = isSubscribedTabRead;
	}
}