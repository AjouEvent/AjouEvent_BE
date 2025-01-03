package com.example.ajouevent.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MemberReadStatusDto {
	private Boolean isTopicTabRead;
	private Boolean isKeywordTabRead;

}