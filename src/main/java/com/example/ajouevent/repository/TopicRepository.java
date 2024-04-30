package com.example.ajouevent.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Topic;

@Repository
public interface TopicRepository extends JpaRepository<Topic, Long> {
	Optional<Topic> findByDepartment(String department);
	boolean existsByDepartment(String department);
}
