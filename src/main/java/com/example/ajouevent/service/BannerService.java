package com.example.ajouevent.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Banner;
import com.example.ajouevent.dto.BannerDto;
import com.example.ajouevent.dto.BannerRequest;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.BannerRepository;
import com.example.ajouevent.util.JsonParsingUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {
	private final BannerRepository bannerRepository;
	private final JsonParsingUtil jsonParsingUtil;

	public BannerDto addBanner(BannerRequest bannerRequest) {
		Banner banner = Banner.builder()
			.imgUrl(bannerRequest.getImgUrl())
			.siteUrl(bannerRequest.getSiteUrl())
			.bannerOrder(bannerRequest.getBannerOrder())
			.startDate(bannerRequest.getStartDate())
			.endDate(bannerRequest.getEndDate())
			.build();
		bannerRepository.save(banner);

		jsonParsingUtil.clearCache("Banners");
		return BannerDto.toDto(banner);
	}

	public void deleteBanner(Long bannerId) {
		Banner banner = bannerRepository.findById(bannerId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.BANNER_NOT_FOUND));
		bannerRepository.delete(banner);

		jsonParsingUtil.clearCache("Banners");
	}

	public List<BannerDto> getAllBanners() {
		String cacheKey = "Banners";
		Optional<List<BannerDto>> cachedData = jsonParsingUtil.getData(cacheKey, new TypeReference<List<BannerDto>>() {});

		if (cachedData.isPresent()) {
			List<BannerDto> response = cachedData.get();
			return response;
		}

		List<Banner> bannerDtoList = bannerRepository.findAllByOrderByBannerOrderAsc();

		jsonParsingUtil.saveData(cacheKey, bannerDtoList, 6, TimeUnit.HOURS);

		return bannerDtoList.stream()
			.map(BannerDto::toDto)
			.collect(Collectors.toList());
	}

	// 기간 지난 배너 삭제
	@Scheduled(cron = "0 0 1 * * ?")
	@Transactional
	public void deleteExpiredBanners() {
		LocalDate now = LocalDate.now();
		bannerRepository.deleteByEndDateBefore(now);

		jsonParsingUtil.clearCache("Banners");
	}

}
