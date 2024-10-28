package com.example.ajouevent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.EventLike;
import com.example.ajouevent.domain.Member;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface EventLikeRepository extends JpaRepository<EventLike, Integer> {
	@EntityGraph(attributePaths = {"clubEvent"})
	List<EventLike> findByMember(Member member);
	boolean existsByMemberAndClubEvent(Member member, ClubEvent clubEvent);
	Optional<EventLike> findByClubEventAndMember(ClubEvent clubEvent, Member member);

	@EntityGraph(attributePaths = {"clubEvent"})
	@Query("SELECT el FROM EventLike el INNER JOIN el.clubEvent ce WHERE el.member = :member")
	List<EventLike> findByMemberWithClubEvent(@Param("member") Member member);

	@Modifying
	@Query("delete from EventLike e where e.eventLikeId in :eventLikeIds")
	void deleteAllByIds(@Param("eventLikeIds") List<Long> eventLikeIds);

}
