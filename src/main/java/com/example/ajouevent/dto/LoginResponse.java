package com.example.ajouevent.dto;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginResponse {

	private String id;
	private String email;
	private String MemberName;
	private LocalDateTime createdAt;

	public LoginResponse(String id, String email, String MemberName, LocalDateTime createdAt) {
		this.id = id;
		this.email = email;
		this.MemberName = MemberName;
		this.createdAt = createdAt;
	}
}