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

	// JOIN FETCH를 사용하여 관련된 Topic을 한 번에 가져옴
	@Query("SELECT tt FROM TopicToken tt JOIN FETCH tt.topic WHERE tt.token IN :tokens")
	List<TopicToken> findTopicTokensWithTopic(@Param("tokens") List<Token> tokens);


	@Modifying
	@Query("delete from TopicToken tt where tt.token.id in :tokenIds")
	void deleteAllByTokenIds(@Param("tokenIds") List<Long> tokenIds);

	@Query("SELECT tt FROM TopicToken tt JOIN FETCH tt.token t WHERE tt.topic = :topic AND t.isDeleted = false")
	List<TopicToken> findByTopicWithValidTokens(@Param("topic") Topic topic);

	@Query("""
	    SELECT tt FROM TopicToken tt
	    JOIN FETCH tt.token t
	    JOIN t.member m
	    JOIN TopicMember tm ON tm.member = m AND tm.topic = :topic
	    WHERE tt.topic = :topic
	    AND t.isDeleted = false
	    AND tm.receiveNotification = true
	""")
	List<TopicToken> findByTopicWithValidTokensAndReceiveNotificationTrue(@Param("topic") Topic topic);

}