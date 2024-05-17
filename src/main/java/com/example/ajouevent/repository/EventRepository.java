package com.example.ajouevent.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.Type;

@Repository
public interface EventRepository extends JpaRepository<ClubEvent, Long> {
	Slice<ClubEvent> findByType(Type type, Pageable pageable);

	Slice<ClubEvent> findByTypeIn(List<Type> types, Pageable pageable);
}