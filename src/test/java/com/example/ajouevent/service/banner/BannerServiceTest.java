package com.example.ajouevent.service.banner;

import com.example.ajouevent.domain.Banner;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.BannerRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BannerServiceTest {

	@Mock
	private BannerRepository bannerRepository;

	@InjectMocks
	private BannerService bannerService;

	@Test
	@DisplayName("ID로 배너를 조회하면 해당 배너를 반환해야 한다")
	void testFindBannerByIdReturnsBanner() {
		// Given
		Banner banner = mock(Banner.class);
		when(bannerRepository.findById(1L)).thenReturn(Optional.of(banner));

		// When
		Banner result = bannerService.findBannerById(1L);

		// Then
		assertThat(result).isEqualTo(banner);
	}

	@Test
	@DisplayName("존재하지 않는 ID로 배너를 조회하면 예외가 발생해야 한다")
	void testFindBannerByIdNotFoundThrowsException() {
		// Given
		when(bannerRepository.findById(1L)).thenReturn(Optional.empty());

		// When & Then
		assertThatThrownBy(() -> bannerService.findBannerById(1L))
			.isInstanceOf(CustomException.class)
			.hasMessage(CustomErrorCode.BANNER_NOT_FOUND.getMessage());
	}

	@Test
	@DisplayName("모든 배너를 조회하면 배너 리스트를 반환해야 한다")
	void testGetAllBannersReturnsList() {
		// Given
		List<Banner> banners = List.of(mock(Banner.class));
		when(bannerRepository.findAllByOrderByBannerOrderAsc()).thenReturn(banners);

		// When
		List<Banner> result = bannerService.getAllBanners();

		// Then
		assertThat(result).isEqualTo(banners);
	}

	@Test
	@DisplayName("배너가 없을 때 모든 배너를 조회하면 빈 리스트를 반환해야 한다")
	void testGetAllBannersReturnsEmptyList() {
		// Given
		when(bannerRepository.findAllByOrderByBannerOrderAsc()).thenReturn(List.of());

		// When
		List<Banner> result = bannerService.getAllBanners();

		// Then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("배너를 추가하면 저장된 배너를 반환해야 한다")
	void testAddBannerSavesBanner() {
		// Given
		Banner banner = mock(Banner.class);
		when(bannerRepository.save(banner)).thenReturn(banner);

		// When
		Banner saved = bannerService.addBanner(banner);

		// Then
		assertThat(saved).isEqualTo(banner);
		verify(bannerRepository).save(banner);
	}

	@Test
	@DisplayName("배너를 삭제하면 저장소에서 삭제되어야 한다")
	void testDeleteBannerDeletesBanner() {
		// Given
		Banner banner = mock(Banner.class);

		// When
		bannerService.deleteBanner(banner);

		// Then
		verify(bannerRepository).delete(banner);
	}

	@Test
	@DisplayName("만료된 배너를 삭제하면 endDate 이전의 배너가 삭제되어야 한다")
	void testDeleteExpiredBannersDeletesByEndDate() {
		// Given
		LocalDate now = LocalDate.of(2025, 8, 6);

		// When
		bannerService.deleteExpiredBanners(now);

		// Then
		verify(bannerRepository).deleteByEndDateBefore(now);
	}
}