package com.example.ajouevent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.EventLike;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Type;

@Repository
public interface EventLikeRepository extends JpaRepository<EventLike, Integer> {
	Slice<EventLike> findByMember(Member member);
	boolean existsByMemberAndClubEvent(Member member, ClubEvent clubEvent);
	Optional<EventLike> findByClubEventAndMember(ClubEvent clubEvent, Member member);
}
