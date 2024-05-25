package com.example.ajouevent.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.ClubEventImage;
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
	private List<String> imgUrl;
	private Long eventId;
	private LocalDateTime createdAt;
	private Type type;
	private String writer;
	private Long likesCount;
	private Boolean star;
	private String subject;
	private String url;

	public static EventDetailResponseDto toDto(ClubEvent clubEvent, Boolean isLiked) {
		List<String> imgUrlList = clubEvent.getClubEventImageList().stream()
			.map(ClubEventImage::getUrl)
			.collect(Collectors.toList());

		return EventDetailResponseDto.builder()
			.eventId(clubEvent.getEventId())
			.title(clubEvent.getTitle())
			.content(clubEvent.getContent())
			.writer(clubEvent.getWriter())
			.createdAt(clubEvent.getCreatedAt())
			.type(clubEvent.getType())
			.imgUrl(imgUrlList)
			.likesCount(clubEvent.getLikesCount())
			.url(clubEvent.getUrl())
			.star(isLiked)
			.type(clubEvent.getType())
			.subject(clubEvent.getType().getKoreanTopic())
			.build();
	}
}