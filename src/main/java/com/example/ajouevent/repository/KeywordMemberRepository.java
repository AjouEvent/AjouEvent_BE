package com.example.ajouevent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.domain.Member;

@Repository
public interface KeywordMemberRepository extends JpaRepository<KeywordMember, Long> {
	boolean existsByKeywordAndMember(Keyword keyword, Member member);
	void deleteByKeywordAndMember(Keyword keyword, Member member);
	List<KeywordMember> findByMember(Member member);
	long countByMember(Member member);

	@Query("SELECT km FROM KeywordMember km " +
		"JOIN FETCH km.keyword k " +
		"JOIN FETCH k.topic t " +
		"WHERE km.member = :member")
	List<KeywordMember> findByMemberWithKeywordAndTopic(@Param("member") Member member);
}