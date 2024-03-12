package com.example.ajouevent.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ErrorDTO {
	private String errorStatus;
	private String errorContent;
	private Object data;

}
