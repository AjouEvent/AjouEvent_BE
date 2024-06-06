package com.example.ajouevent.dto;

import static org.bouncycastle.asn1.x500.style.RFC4519Style.*;

import java.time.LocalDate;
import java.util.List;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.EventBanner;
import com.fasterxml.jackson.annotation.JsonCreator;

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
public class EventBannerDto {
	private String imgUrl;
	private String url;
	private Long imgOrder;
	private LocalDate startDate;
	private LocalDate endDate;

	public static EventBannerDto toDto(EventBanner eventBanner) {
		return EventBannerDto.builder()
			.imgUrl(eventBanner.getClubEvent().getClubEventImageList().get(0).getUrl())
			.url("https://ajou-event.vercel.app/event/" + eventBanner.getClubEvent().getEventId())
			.imgOrder(eventBanner.getImgOrder())
			.startDate(eventBanner.getStartDate())
			.endDate(eventBanner.getEndDate())
			.build();
	}
}
