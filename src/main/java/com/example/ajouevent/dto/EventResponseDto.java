package com.example.ajouevent.dto;

import com.example.ajouevent.domain.ClubEvent;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EventResponseDto { // 게시글 상세 조회시에 나오는 이벤트 정보
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