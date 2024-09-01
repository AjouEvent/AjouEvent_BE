package com.example.ajouevent.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.EventBanner;

@Repository
public interface EventBannerRepository extends JpaRepository<EventBanner, Long> {
	// bannerOrder 순서대로 모든 EventBanner를 조회하는 메서드
	List<EventBanner> findAllByOrderByBannerOrderAsc();

	// 기간 끝난 배너 이벤트 삭제
	void deleteByEndDateBefore(LocalDate endDate);

}
