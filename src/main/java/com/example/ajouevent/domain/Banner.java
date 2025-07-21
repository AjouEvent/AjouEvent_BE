package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "banner")
public class Banner extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bannerId;

	@Column(nullable = false)
	private Long bannerOrder;

	@Column(nullable = false)
	private String imgUrl;

	@Column(nullable = false)
	private String siteUrl;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

	@Builder
	private Banner(String imgUrl, String siteUrl, Long bannerOrder, LocalDate startDate, LocalDate endDate) {
		this.imgUrl = imgUrl;
		this.siteUrl = siteUrl;
		this.bannerOrder = bannerOrder;
		this.startDate = startDate;
		this.endDate = endDate;
	}

	public static Banner create(String imgUrl, String siteUrl, Long bannerOrder, LocalDate startDate,
		LocalDate endDate) {
		return Banner.builder()
			.imgUrl(imgUrl)
			.siteUrl(siteUrl)
			.bannerOrder(bannerOrder)
			.startDate(startDate)
			.endDate(endDate)
			.build();
	}

}
