package com.example.ajouevent.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.ajouevent.domain.Type;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateEventRequest {
	private String title;
	private String content;
	private String url;
	private String writer;
	private String subject;
	private Type type;
	private LocalDateTime eventDateTime;
	private List<String> imageUrls;  // 이미지 URL 목록 추가

	@Builder
	public UpdateEventRequest(String title, String content, String url, String writer, String subject, Type type,
		LocalDateTime eventDateTime, List<String> imageUrls) {
		this.title = title;
		this.content = content;
		this.url = url;
		this.writer = writer;
		this.subject = subject;
		this.type = type;
		this.eventDateTime = eventDateTime;
		this.imageUrls = imageUrls;
	}
}
