package com.example.ajouevent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordToken;

@Repository
public interface KeywordTokenRepository extends JpaRepository<KeywordToken, Long> {

	@Modifying
	@Query("DELETE FROM KeywordToken kt WHERE kt.keyword = :keyword")
	void deleteByKeyword(Keyword keyword);
}
