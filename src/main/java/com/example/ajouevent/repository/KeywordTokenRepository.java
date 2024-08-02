package com.example.ajouevent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordToken;
import com.example.ajouevent.domain.Token;

@Repository
public interface KeywordTokenRepository extends JpaRepository<KeywordToken, Long> {

	@Modifying
	@Query("DELETE FROM KeywordToken kt WHERE kt.keyword = :keyword AND kt.token IN :tokens")
	void deleteByKeywordAndTokens(@Param("keyword") Keyword keyword, @Param("tokens") List<Token> tokens);
}
