package com.example.ajouevent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Alarm;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
}