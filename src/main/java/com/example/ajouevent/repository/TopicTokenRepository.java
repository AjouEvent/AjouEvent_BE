package com.example.ajouevent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.TopicToken;

@Repository
public interface TopicTokenRepository extends JpaRepository<TopicToken, Long> {
}