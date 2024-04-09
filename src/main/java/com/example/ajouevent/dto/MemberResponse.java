package com.example.ajouevent.dto;

import lombok.Getter;

@Getter
public class MemberResponse {

	private final String id;
	private final String MemberName;
	private final String email;

	public MemberResponse(String id, String MemberName, String email) {
		this.id = id;
		this.MemberName = MemberName;
		this.email = email;
	}
}
