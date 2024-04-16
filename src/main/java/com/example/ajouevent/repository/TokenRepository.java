package com.example.ajouevent.repository;

import com.example.ajouevent.domain.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
	Token findByValue(String value);
	boolean existsByValue(String value);
}

