package com.example.ajouevent.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class MemberDTO {

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
	public static class MemberInfoDto {

		private String email;

		@Builder
		public MemberInfoDto(String email) {
			this.email = email;
		}

	}
}
