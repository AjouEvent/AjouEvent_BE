package com.example.ajouevent.dto;

import com.example.ajouevent.domain.Role;
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
		private Role role;

		@Builder
		public MemberInfoDto(Long memberId, String email, String password, Role role) {
			this.memberId = memberId;
			this.email = email;
			this.password = password;
			this.role = role;
		}

	}
}
