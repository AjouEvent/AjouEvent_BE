package com.example.ajouevent.dto;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
public class ResponseDto {

	private HttpStatus successStatus;
	private String successContent;
	private Object Data;

}
