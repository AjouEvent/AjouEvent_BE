package com.example.ajouevent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicToken;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface TopicTokenRepository extends JpaRepository<TopicToken, Long> {

	@Modifying
	@Query("DELETE FROM TopicToken tt WHERE tt.topic = :topic AND tt.token IN :tokens")
	void deleteByTopicAndTokens(@Param("topic") Topic topic, @Param("tokens") List<Token> tokens);

	// 토큰 리스트를 기반으로 TopicToken 객체들을 조회
	List<TopicToken> findByTokenIn(List<Token> tokens);

	@Modifying
	@Query("delete from TopicToken tt where tt.token.id in :tokenIds")
	void deleteAllByTokenIds(@Param("tokenIds") List<Long> tokenIds);

}