package com.example.ajouevent.dto;

import java.time.LocalDate;
import com.example.ajouevent.domain.Banner;
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
public class BannerDto {
	private String imgUrl;
	private String siteUrl;
	private Long bannerOrder;
	private LocalDate startDate;
	private LocalDate endDate;

	public static BannerDto toDto(Banner banner) {
		return BannerDto.builder()
			.imgUrl(banner.getImgUrl())
			.siteUrl(banner.getSiteUrl())
			.bannerOrder(banner.getBannerOrder())
			.startDate(banner.getStartDate())
			.endDate(banner.getEndDate())
			.build();
	}
}
