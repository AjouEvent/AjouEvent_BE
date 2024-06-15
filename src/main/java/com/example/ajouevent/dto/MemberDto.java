package com.example.ajouevent.dto;

import com.example.ajouevent.domain.Role;
import lombok.*;

@Getter
@Setter
public class MemberDto {

	@Data
	@Builder
	@AllArgsConstructor
	@NoArgsConstructor
	@Getter
	@Setter
	public static class LoginRequest {

		private String email;
		private String password;
		private String fcmToken;

	}

	@Getter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class MemberInfoDto {
		private Long memberId;
		private String email;
		private String password;
		private Role role;

	}
}
