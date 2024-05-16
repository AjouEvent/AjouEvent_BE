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
public class EventDetailResponseDto { // 게시글 상세 조회시에 나오는 이벤트 정보
	private String title;
	private String content;
	private String imgUrl;
	private Long eventId;
	private LocalDateTime createdAt;
	private Type type;
	private String writer;
	private Long likesCount;
	private Boolean star;

	public static EventDetailResponseDto toDto(ClubEvent clubEvent, Boolean isLiked) {
		return EventDetailResponseDto.builder()
			.eventId(clubEvent.getEventId())
			.title(clubEvent.getTitle())
			.content(clubEvent.getContent())
			.writer(clubEvent.getWriter())
			.createdAt(clubEvent.getCreatedAt())
			.type(clubEvent.getType())
			.imgUrl(clubEvent.getClubEventImageList().get(0).getUrl())
			.likesCount(clubEvent.getLikesCount())
			.star(isLiked)
			.build();
	}
}