package com.example.ajouevent.dto;

import lombok.*;

public class MemberDto {

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	public static class LoginRequest {

		private String email;
		private String password;
		private String fcmToken;

	}

	@Getter
	@NoArgsConstructor(access = AccessLevel.PROTECTED)
	@Builder
	public static class MemberInfoDto {
		private Long memberId;
		private String email;
		private String password;
		private String role;

		@Builder
		public MemberInfoDto(Long memberId, String email, String password, String role) {
			this.memberId = memberId;
			this.email = email;
			this.password = password;
			this.role = role;
		}

	}
}
