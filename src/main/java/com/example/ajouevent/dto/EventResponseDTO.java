package com.example.ajouevent.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.ClubEventImage;
import com.example.ajouevent.domain.Type;

import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EventResponseDTO {
	private String title;
	private String imgUrl;
	private Long eventId;
	private Boolean star;

	public static EventResponseDTO toDto(ClubEvent clubEvent) {
		return EventResponseDTO.builder()
			.eventId(clubEvent.getEventId())
			.title(clubEvent.getTitle())
			.imgUrl(clubEvent.getClubEventImageList().get(0).getUrl())
			.build();
	}
}