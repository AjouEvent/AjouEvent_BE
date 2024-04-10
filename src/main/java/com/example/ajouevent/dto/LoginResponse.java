package com.example.ajouevent.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class LoginResponse {

	private Long id;
	private String grantType;
	private String accessToken;
	private String refreshToken;

	@Builder
	public LoginResponse(Long id, String grantType, String accessToken, String refreshToken) {
		this.id = id;
		this.grantType = grantType;
		this.accessToken = accessToken;
		this.refreshToken = refreshToken;
	}
}