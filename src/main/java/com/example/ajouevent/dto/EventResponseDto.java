package com.example.ajouevent.dto;

import com.example.ajouevent.domain.ClubEvent;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EventResponseDto {
	private String title;
	private String imgUrl;
	private Long eventId;
	private Boolean star;

	public static EventResponseDto toDto(ClubEvent clubEvent) {
		return EventResponseDto.builder()
			.eventId(clubEvent.getEventId())
			.title(clubEvent.getTitle())
			.imgUrl(clubEvent.getClubEventImageList().get(0).getUrl())
			.build();
	}
}