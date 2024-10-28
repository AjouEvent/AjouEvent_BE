package com.example.ajouevent.dto;

import java.time.LocalDate;
import com.example.ajouevent.domain.EventBanner;

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
	private String siteUrl;
	private Long bannerOrder;
	private LocalDate startDate;
	private LocalDate endDate;

	public static EventBannerDto toDto(EventBanner eventBanner) {
		return EventBannerDto.builder()
			.imgUrl(eventBanner.getImgUrl())
			.siteUrl(eventBanner.getSiteUrl())
			.bannerOrder(eventBanner.getBannerOrder())
			.startDate(eventBanner.getStartDate())
			.endDate(eventBanner.getEndDate())
			.build();
	}
}
