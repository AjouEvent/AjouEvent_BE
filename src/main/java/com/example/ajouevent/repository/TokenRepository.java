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
	Token findByValue(String value);
	Optional<Token> findByValueAndMember(String value, Member member);
	boolean existsByValue(String value);
	List<Token> findByMemberEmail(String Email);

	void deleteByMember(Member m);

	List<Token> findByExpirationDate(LocalDate now);

	List<Token> findByMember(Member m);
}

