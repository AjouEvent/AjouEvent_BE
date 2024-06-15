package com.example.ajouevent.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

	private Long id;
	private String grantType;
	private String accessToken;
	private String refreshToken;
	private String name;
	private String major;
	private String email;
}