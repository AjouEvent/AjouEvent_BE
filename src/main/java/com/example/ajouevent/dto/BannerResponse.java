package com.example.ajouevent.dto;

import java.time.LocalDate;

import com.example.ajouevent.domain.Banner;

import lombok.Builder;

@Builder
public record BannerResponse(
	String imgUrl,
	String siteUrl,
	Long bannerOrder,
	LocalDate startDate,
	LocalDate endDate
) {
	public static BannerResponse fromEntity(Banner banner) {
		return BannerResponse.builder()
			.imgUrl(banner.getImgUrl())
			.siteUrl(banner.getSiteUrl())
			.bannerOrder(banner.getBannerOrder())
			.startDate(banner.getStartDate())
			.endDate(banner.getEndDate())
			.build();
	}
}
