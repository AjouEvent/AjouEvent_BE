package com.example.ajouevent.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.ajouevent.domain.ClubEventImage;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class NoticeDto {

	@NotNull(message = "제목은 Null 일 수 없습니다!")
	private String title;

	private String content;

	private String category;

	private String department;

	private String englishTopic;

	private String koreanTopic;

	private String url;

	private List<String> images;

	private LocalDateTime date;


}