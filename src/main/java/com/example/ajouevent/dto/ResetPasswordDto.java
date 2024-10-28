package com.example.ajouevent.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResetPasswordDto {
	private String newPassword;
	private String email;
}
