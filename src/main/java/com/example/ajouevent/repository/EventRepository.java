package com.example.ajouevent.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.Type;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface EventRepository extends JpaRepository<ClubEvent, Long> {
	Slice<ClubEvent> findByTypeAndTitleContaining(Type type, String keyword, Pageable pageable);

	Slice<ClubEvent> findByTypeIn(List<Type> types, Pageable pageable);

	Slice<ClubEvent> findAllByTitleContaining(String keyword, Pageable pageable);

	@Query("SELECT ce FROM ClubEvent ce WHERE ce.eventId IN :eventIds ORDER BY ce.createdAt DESC")
	Slice<ClubEvent> findByEventIds(@Param("eventIds") List<Long> eventIds, Pageable pageable);

	@Query("SELECT ce FROM ClubEvent ce WHERE ce.createdAt BETWEEN :startOfWeek AND :endOfWeek ORDER BY ce.viewCount DESC")
	List<ClubEvent> findTop10ByCreatedAtBetweenOrderByViewCountDesc(@Param("startOfWeek") LocalDateTime startOfWeek, @Param("endOfWeek") LocalDateTime endOfWeek);

	@Modifying
	@Query("UPDATE ClubEvent e SET e.viewCount = :viewCount WHERE e.eventId = :eventId")
	void updateViews(@Param("viewCount") Long viewCount, @Param("eventId") Long eventId);

	List<ClubEvent> findTop10ByOrderByViewCountDesc();
}