package com.example.ajouevent.facade;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Banner;
import com.example.ajouevent.dto.BannerRequest;
import com.example.ajouevent.dto.BannerResponse;
import com.example.ajouevent.service.banner.BannerCacheService;
import com.example.ajouevent.service.banner.BannerService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerFacade {

	private final BannerService bannerService;
	private final BannerCacheService bannerCacheService;

	public List<BannerResponse> getAllBanners() {
		Optional<List<BannerResponse>> cachedBanners = bannerCacheService.get();
		if (cachedBanners.isPresent()) {
			log.debug("✅ Banner cache hit");
			return cachedBanners.get();
		}

		log.debug("⚠️ Banner cache miss - loading from DB");
		List<BannerResponse> bannerDtoList = bannerService.getAllBanners().stream()
			.map(BannerResponse::fromEntity)
			.toList();

		bannerCacheService.set(bannerDtoList);

		return bannerDtoList;
	}

	public BannerResponse addBanner(BannerRequest request) {
		Banner banner = Banner.create(
			request.getImgUrl(),
			request.getSiteUrl(),
			request.getBannerOrder(),
			request.getStartDate(),
			request.getEndDate()
		);
		Banner saved = bannerService.addBanner(banner);
		bannerCacheService.delete();
		return BannerResponse.fromEntity(saved);
	}

	public void deleteBanner(Long bannerId) {
		Banner banner = bannerService.findBannerById(bannerId);
		bannerService.deleteBanner(banner);
		bannerCacheService.delete();
	}

	@Scheduled(cron = "0 0 1 * * ?")
	@Transactional
	public void deleteExpiredBanners() {
		LocalDate now = LocalDate.now();
		bannerService.deleteExpiredBanners(now);
		bannerCacheService.delete();
	}
}
