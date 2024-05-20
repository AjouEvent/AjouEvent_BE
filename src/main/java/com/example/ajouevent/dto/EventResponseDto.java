package com.example.ajouevent.dto;

import java.time.LocalDateTime;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.Type;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class EventResponseDto { // 게시글 홈 화면 조회 시에 나오는 이벤트 정보
	private String title;
	private String imgUrl;
	private String url;
	private LocalDateTime createdAt;
	private Long eventId;
	private Long likesCount;
	private Boolean star;
	private String subject;
	private Type type;

	public static EventResponseDto toDto(ClubEvent clubEvent) {
		return EventResponseDto.builder()
			.eventId(clubEvent.getEventId())
			.title(clubEvent.getTitle())
			.imgUrl(clubEvent.getClubEventImageList().get(0).getUrl())
			.url(clubEvent.getUrl())
			.createdAt(clubEvent.getCreatedAt())
			.likesCount(clubEvent.getLikesCount())
			.type(clubEvent.getType())
			.subject(clubEvent.getType().getKoreanTopic())
			.build();
	}
}