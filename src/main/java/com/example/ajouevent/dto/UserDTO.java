package com.example.ajouevent.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserDTO {

	@Getter
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class LoginRequest {

		private String email;
		private String password;
		private String token;

	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class UserInfoDto {

		private String email;

		@Builder
		public UserInfoDto(String email) {
			this.email = email;
		}

	}
}
