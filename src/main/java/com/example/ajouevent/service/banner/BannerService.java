package com.example.ajouevent.service.banner;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Banner;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.BannerRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BannerService {
	private final BannerRepository bannerRepository;

	@Transactional(readOnly = true)
	public Banner findBannerById(Long bannerId) {
		return bannerRepository.findById(bannerId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.BANNER_NOT_FOUND));
	}

	@Transactional(readOnly = true)
	public List<Banner> getAllBanners() {
		return bannerRepository.findAllByOrderByBannerOrderAsc();
	}

	@Transactional
	public Banner addBanner(Banner banner) {
		return bannerRepository.save(banner);
	}

	@Transactional
	public void deleteBanner(Banner banner) {
		bannerRepository.delete(banner);
	}

	@Transactional
	public void deleteExpiredBanners(LocalDate now) {
		bannerRepository.deleteByEndDateBefore(now);
	}
}