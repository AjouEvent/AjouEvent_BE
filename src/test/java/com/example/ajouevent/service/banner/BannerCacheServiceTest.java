package com.example.ajouevent.service.banner;

import com.example.ajouevent.domain.Banner;
import com.example.ajouevent.dto.BannerResponse;
import com.example.ajouevent.infrastructure.cache.CacheRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BannerCacheServiceTest {

	@Mock
	private CacheRepository cacheRepository;

	@InjectMocks
	private BannerCacheService bannerCacheService;

	@Test
	@DisplayName("배너 캐시를 조회하면 Optional로 반환해야 한다")
	void testGetBannerCacheReturnsOptional() {
		// given
		List<BannerResponse> cached = List.of(BannerResponse.fromEntity(mock(Banner.class)));
		when(cacheRepository.get(eq("Banners"), any())).thenReturn(Optional.of(cached));

		// when
		Optional<List<BannerResponse>> result = bannerCacheService.get();

		// then
		assertThat(result).isPresent();
		assertThat(result.get()).hasSize(1);
		verify(cacheRepository).get(eq("Banners"), any());
	}

	@Test
	@DisplayName("배너 캐시를 저장하면 캐시에 저장되어야 한다")
	void testSetBannerCache() {
		// given
		List<BannerResponse> banners = List.of(BannerResponse.fromEntity(mock(Banner.class)));

		// when
		bannerCacheService.set(banners);

		// then
		verify(cacheRepository).set(eq("Banners"), eq(banners), eq(6L), eq(TimeUnit.HOURS));
	}

	@Test
	@DisplayName("배너 캐시를 삭제하면 캐시에서 삭제되어야 한다")
	void testDeleteBannerCache() {
		// when
		bannerCacheService.delete();

		// then
		verify(cacheRepository).delete("Banners");
	}

	@Test
	@DisplayName("캐시에 값이 없으면 Optional.empty()를 반환해야 한다")
	void testGetBannerCacheReturnsEmptyWhenNoCache() {
		// given
		when(cacheRepository.get(eq("Banners"), any())).thenReturn(Optional.empty());

		// when
		Optional<List<BannerResponse>> result = bannerCacheService.get();

		// then
		assertThat(result).isEmpty();
		verify(cacheRepository).get(eq("Banners"), any());
	}

}