package com.example.ajouevent.dto;

import java.time.LocalDateTime;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.Type;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventResponseDto { // 게시글 홈 화면 조회 시에 나오는 이벤트 정보
	private String title;
	private String content;
	private String imgUrl;
	private String url;
	private LocalDateTime createdAt;
	private Long eventId;
	private Long likesCount;
	private Long viewCount;
	private Boolean star;
	private String subject;
	private Type type;
	private String writer;

	public static EventResponseDto toDto(ClubEvent clubEvent) {
		return EventResponseDto.builder()
			.eventId(clubEvent.getEventId())
			.title(clubEvent.getTitle())
			.content(clubEvent.getContent())
			.imgUrl(clubEvent.getClubEventImageList().get(0).getUrl())
			.url(clubEvent.getUrl())
			.createdAt(clubEvent.getCreatedAt())
			.likesCount(clubEvent.getLikesCount())
			.viewCount(clubEvent.getViewCount())
			.type(clubEvent.getType())
			.subject(clubEvent.getType().getKoreanTopic())
			.writer(clubEvent.getWriter())
			.build();
	}
}