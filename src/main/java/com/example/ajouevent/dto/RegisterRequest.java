package com.example.ajouevent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@AllArgsConstructor
public class RegisterRequest {

	@NotBlank(message = "이름을 입력해주세요.")
	private String name;

	@NotBlank(message = "이메일을 입력해주세요.")
	@Email
	private String email;

	@NotBlank(message = "비밀번호를 입력해주세요")
	@Pattern(message = "비밀번호는 영문 소문자와 숫자를 포함한 8자 이상이어야 합니다.",
		regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,64}$")
	private String password;

	private String major;

	// private String phone;
}