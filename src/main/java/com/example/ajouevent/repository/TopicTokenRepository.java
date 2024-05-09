package com.example.ajouevent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicToken;

@Repository
public interface TopicTokenRepository extends JpaRepository<TopicToken, Long> {
	void deleteByTopic(Topic topic);
	List<TopicToken> findByToken(Token token);

	// 토큰 리스트를 기반으로 TopicToken 객체들을 조회
	List<TopicToken> findByTokenIn(List<Token> tokens);


}