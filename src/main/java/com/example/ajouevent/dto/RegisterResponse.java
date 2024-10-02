package com.example.ajouevent.dto;

import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RegisterResponse {

	@Email
	private String email;

	private String name;

}