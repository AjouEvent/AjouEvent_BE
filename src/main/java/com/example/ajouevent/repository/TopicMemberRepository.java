package com.example.ajouevent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.TopicMember;

@Repository
public interface TopicMemberRepository extends JpaRepository<TopicMember, Long> {
	List<TopicMember> findByMember(Member member);

	void deleteByMember(Optional<Member> member);
}