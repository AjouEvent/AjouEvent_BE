package com.example.ajouevent.dto;

import java.util.List;

import com.example.ajouevent.domain.Token;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TopicRequest {
	private String topic;
	private List<Token> tokens;
}
