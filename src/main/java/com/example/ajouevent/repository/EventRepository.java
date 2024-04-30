package com.example.ajouevent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.Type;

@Repository
public interface EventRepository extends JpaRepository<ClubEvent, Long> {
	List<ClubEvent> findByType(Type type);
}