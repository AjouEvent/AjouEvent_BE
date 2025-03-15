package com.example.ajouevent.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Banner;

@Repository
public interface BannerRepository extends JpaRepository<Banner, Long> {
	// bannerOrder 순서대로 모든 Banner를 조회하는 메서드
	List<Banner> findAllByOrderByBannerOrderAsc();

	// 기간 끝난 배너 이벤트 삭제
	void deleteByEndDateBefore(LocalDate endDate);

}
