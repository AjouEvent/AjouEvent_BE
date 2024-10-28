package com.example.ajouevent.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class KeywordResponse {
	private String encodedKeyword;
	private String koreanKeyword;
	private String searchKeyword;
	private String topicName;
	private Boolean isRead; // 읽음 상태 필드 추가
	private LocalDateTime lastReadAt;  // 마지막 읽음 시간 필드 추가
}