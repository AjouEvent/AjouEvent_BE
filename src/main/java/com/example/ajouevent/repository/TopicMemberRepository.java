package com.example.ajouevent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface TopicMemberRepository extends JpaRepository<TopicMember, Long> {
	List<TopicMember> findByMember(Member member);

	boolean existsByTopicAndMember(Topic topic, Member member);

	// Member와 Topic으로 TopicMember 삭제
	void deleteByTopicAndMember(Topic topic, Member member);

	@Modifying
	@Query("delete from TopicMember c where c.id in :ids")
	void deleteAllByIds(@Param("ids") List<Long> ids);

	@Query("SELECT tm FROM TopicMember tm JOIN FETCH tm.topic WHERE tm.member = :member")
	List<TopicMember> findByMemberWithTopic(@Param("member") Member member);

	@Query("SELECT tm.member FROM TopicMember tm WHERE tm.topic = :topic")
	List<Member> findMembersByTopic(@Param("topic") Topic topic);

	@Query("SELECT tm FROM TopicMember tm WHERE tm.topic = :topic")
	List<TopicMember> findByTopic(@Param("topic") Topic topic);

	Optional<TopicMember> findByMemberAndTopic(Member member, Topic topic);

	boolean existsByMemberAndIsReadFalse(Member member);

	@Query("SELECT tm FROM TopicMember tm JOIN FETCH tm.member m JOIN FETCH m.tokens t WHERE tm.topic = :topic AND tm.receiveNotification = true ")
	List<TopicMember> findByTopicWithNotificationEnabledAndTokens(@Param("topic") Topic topic);

	@Query("SELECT tm.receiveNotification FROM TopicMember tm WHERE tm.member = :member AND tm.topic = :topic")
	boolean findReceiveNotificationByMemberAndTopic(@Param("member") Member member, @Param("topic") Topic topic);

}