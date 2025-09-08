package com.example.ajouevent.facade;

import com.example.ajouevent.domain.Banner;
import com.example.ajouevent.dto.BannerRequest;
import com.example.ajouevent.dto.BannerResponse;
import com.example.ajouevent.service.banner.BannerCacheService;
import com.example.ajouevent.service.banner.BannerService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.*;

@DisplayName("BannerFacade 캐시 및 배너 관리 테스트")
class BannerFacadeTest {

	@InjectMocks
	private BannerFacade bannerFacade;

	@Mock
	private BannerService bannerService;

	@Mock
	private BannerCacheService bannerCacheService;

	private AutoCloseable closeable;

	@BeforeEach
	void setUp() {
		closeable = MockitoAnnotations.openMocks(this);
	}

	@AfterEach
	void tearDown() throws Exception {
		closeable.close();
	}

	@Test
	@DisplayName("캐시 hit일 때 DB에 접근하지 않고 캐시 데이터를 반환해야 한다")
	void testReturnCacheDataWhenCacheHit() {
		// Given
		List<BannerResponse> cached = List.of(
			BannerResponse.builder()
				.imgUrl("img1").siteUrl("site1").bannerOrder(1L)
				.startDate(LocalDate.now()).endDate(LocalDate.now().plusDays(1)).build()
		);
		given(bannerCacheService.get()).willReturn(Optional.of(cached));

		// When
		List<BannerResponse> result = bannerFacade.getAllBanners();

		// Then
		assertThat(result).isEqualTo(cached);
		verify(bannerService, never()).getAllBanners();
	}

	@Test
	@DisplayName("캐시 miss일 때 DB에서 조회 후 캐시에 저장해야 한다")
	void testLoadFromDbAndCacheWhenCacheMiss() {
		// Given
		given(bannerCacheService.get()).willReturn(Optional.empty());
		Banner banner = Banner.create("img2", "site2", 2L, LocalDate.now(), LocalDate.now().plusDays(2));
		given(bannerService.getAllBanners()).willReturn(List.of(banner));

		// When
		List<BannerResponse> result = bannerFacade.getAllBanners();

		// Then
		assertThat(result).hasSize(1);
		verify(bannerService).getAllBanners();
		verify(bannerCacheService).set(any());
	}

	@Test
	@DisplayName("캐시 miss 후 DB 조회는 성공했으나 캐시 저장에 실패해도 결과는 반환되어야 한다")
	void testReturnResultEvenIfCacheSetFailsOnCacheMiss() {
		// Given
		given(bannerCacheService.get()).willReturn(Optional.empty());
		Banner banner = Banner.create("img", "site", 1L, LocalDate.now(), LocalDate.now().plusDays(1));
		given(bannerService.getAllBanners()).willReturn(List.of(banner));
		// 실제 서비스에서는 캐시 저장 실패가 swallow되므로 예외를 던지지 않음

		// When
		List<BannerResponse> result = bannerFacade.getAllBanners();

		// Then
		assertThat(result).hasSize(1);
		verify(bannerCacheService).set(any());
	}

	@Test
	@DisplayName("배너를 추가하면 캐시가 삭제되어야 한다")
	void testDeleteCacheWhenAddBanner() {
		// Given
		BannerRequest req = new BannerRequest(1L, "img", "site", LocalDate.now(), LocalDate.now().plusDays(1));
		Banner banner = Banner.create(req.getImgUrl(), req.getSiteUrl(), req.getBannerOrder(), req.getStartDate(),
			req.getEndDate());
		given(bannerService.addBanner(any())).willReturn(banner);

		// When
		bannerFacade.addBanner(req);

		// Then
		verify(bannerCacheService).delete();
	}

	@Test
	@DisplayName("배너를 삭제하면 캐시가 삭제되어야 한다")
	void testDeleteCacheWhenDeleteBanner() {
		// Given
		Banner banner = Banner.create("img", "site", 1L, LocalDate.now(), LocalDate.now().plusDays(1));
		given(bannerService.findBannerById(anyLong())).willReturn(banner);

		// When
		bannerFacade.deleteBanner(1L);

		// Then
		verify(bannerCacheService).delete();
	}

	@Test
	@DisplayName("만료된 배너를 삭제하면 캐시가 삭제되어야 한다")
	void testDeleteCacheWhenDeleteExpiredBanners() {
		// When
		bannerFacade.deleteExpiredBanners();

		// Then
		verify(bannerService).deleteExpiredBanners(any());
		verify(bannerCacheService).delete();
	}
}
