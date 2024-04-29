package com.example.ajouevent.domain;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.AlarmImage;

@Getter
@Builder
public class AlarmDto {
	private LocalDateTime alarmDateTime;
	private String date;
	private String title;
	private String content;
	private String url;
	private String writer;
	private String subject;
	private String target;
	private List<String> alarmImageUrls;
	private List<AlarmImage> alarmImageList;

	// toEntity 메서드 추가
	public Alarm toEntity() {
		return Alarm.builder()
			.alarmDateTime(alarmDateTime)
			.date(date)
			.title(title)
			.content(content)
			.url(url)
			.writer(writer)
			.subject(subject)
			.target(target)
			.alarmImageList(alarmImageList)
			.build();
	}

	public Alarm toEntity2(List<AlarmImage> alarmImageList2) {
		return Alarm.builder()
			.alarmDateTime(alarmDateTime)
			.date(date)
			.title(title)
			.content(content)
			.url(url)
			.writer(writer)
			.subject(subject)
			.target(target)
			.alarmImageList(alarmImageList2)
			.build();
	}

}
