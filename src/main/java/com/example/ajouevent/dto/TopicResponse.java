package com.example.ajouevent.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopicResponse {
	private final Long id;
	private final String koreanTopic;
	private final String englishTopic;
	private Boolean isRead; // 읽음 상태 필드 추가
	private LocalDateTime lastReadAt;  // 마지막 읽음 시간 필드 추가
}
