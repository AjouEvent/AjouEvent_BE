package com.example.ajouevent.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface TokenRepository extends JpaRepository<Token, Long> {
	Optional<Token> findByTokenValueAndMember(String value, Member member);
	List<Token> findByExpirationDate(LocalDate now);
	List<Token> findByMember(Member member);

	@Modifying
	@Query("delete from Token t where t.id in :tokenIds")
	void deleteAllByTokenIds(@Param("tokenIds") List<Long> tokenIds);
}

