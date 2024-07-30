package com.example.ajouevent.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
	Optional<Token> findByTokenValueAndMember(String value, Member member);
	List<Token> findByExpirationDate(LocalDate now);
}

