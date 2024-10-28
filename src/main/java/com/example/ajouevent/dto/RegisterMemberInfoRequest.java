package com.example.ajouevent.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterMemberInfoRequest {

	private String major;
	@JsonCreator
	public RegisterMemberInfoRequest(String major) {
		this.major = major;
	}

}