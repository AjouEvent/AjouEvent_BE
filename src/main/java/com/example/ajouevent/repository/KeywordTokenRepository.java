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

	@Modifying
	@Query("DELETE FROM KeywordToken kt WHERE kt.token.id IN :tokenIds")
	void deleteAllByTokenIds(@Param("tokenIds") List<Long> tokenIds);

	// JOIN FETCH를 사용하여 관련된 Keyword를 한 번에 가져옴
	@Query("SELECT kt FROM KeywordToken kt JOIN FETCH kt.keyword WHERE kt.token IN :tokens")
	List<KeywordToken> findKeywordTokensWithKeyword(@Param("tokens") List<Token> tokens);

	@Query("SELECT kt FROM KeywordToken kt JOIN FETCH kt.token t WHERE kt.keyword = :keyword AND t.isDeleted = false")
	List<KeywordToken> findKeywordTokensWithTokenByKeyword(@Param("keyword") Keyword keyword);
}
