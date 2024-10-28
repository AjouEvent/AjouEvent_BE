package com.example.ajouevent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
public class CurrentPasswordDto {

	private String currentPassword;

	@JsonCreator
	public CurrentPasswordDto(String currentPassword) {
		this.currentPassword = currentPassword;
	}
}
