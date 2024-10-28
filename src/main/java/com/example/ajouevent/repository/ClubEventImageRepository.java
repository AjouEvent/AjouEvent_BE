package com.example.ajouevent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.ClubEventImage;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface ClubEventImageRepository extends JpaRepository<ClubEventImage, Long> {
	@Modifying
	@Transactional
	@Query("DELETE FROM ClubEventImage c WHERE c.url IN :urls")
	void deleteClubEventImagesByUrls(@Param("urls") List<String> urls);
}
