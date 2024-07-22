package com.example.ajouevent.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Alarm;

@Repository
public interface AlarmRepository extends JpaRepository<Alarm, Long> {
	@Query("SELECT a FROM Alarm a WHERE a.alarmDateTime = :alarmDateTime")
	List<Alarm> findAlarmsByDateTime(@Param("alarmDateTime") LocalDateTime alarmDateTime);
}