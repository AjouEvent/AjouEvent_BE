package com.example.ajouevent.dto;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ValidErrorDTO {
	private String errorCode;
	private List<String> errorContent;
}
