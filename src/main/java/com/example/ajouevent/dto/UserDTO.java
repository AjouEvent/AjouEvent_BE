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
	@Builder
	public static class UserInfoDto {

		private Long memberId;
		private String email;
		private String password;
		private String role;

		@Builder
		public UserInfoDto(Long memberId, String email, String password, String role) {
			this.memberId = memberId;
			this.email = email;
			this.password = password;
			this.role = role;
		}

	}
}
