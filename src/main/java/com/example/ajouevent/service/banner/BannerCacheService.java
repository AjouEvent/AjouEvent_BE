package com.example.ajouevent.service.banner;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.example.ajouevent.dto.BannerResponse;
import com.example.ajouevent.infrastructure.cache.CacheRepository;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BannerCacheService {

	private final CacheRepository cacheRepository;
	private static final String BANNER_CACHE_KEY = "Banners";

	public Optional<List<BannerResponse>> get() {
		return cacheRepository.get(BANNER_CACHE_KEY, new TypeReference<>() {
		});
	}

	public void set(List<BannerResponse> banners) {
		cacheRepository.set(BANNER_CACHE_KEY, banners, 6, TimeUnit.HOURS);
	}

	public void delete() {
		cacheRepository.delete(BANNER_CACHE_KEY);
	}
}