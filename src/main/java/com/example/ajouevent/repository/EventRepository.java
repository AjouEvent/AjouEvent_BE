package com.example.ajouevent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.ClubEvent;

@Repository
public interface EventRepository extends JpaRepository<ClubEvent, Long> {
}