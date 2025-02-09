package com.example.ajouevent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

	@Modifying
	@Query("DELETE FROM KeywordMember c WHERE c.id IN :ids")
	void deleteAllByIds(@Param("ids") List<Long> ids);

	@Query("SELECT km FROM KeywordMember km JOIN FETCH km.keyword WHERE km.member = :member")
	List<KeywordMember> findByMemberWithKeyword(@io.lettuce.core.dynamic.annotation.Param("member") Member member);

	@Query("SELECT km FROM KeywordMember km WHERE km.keyword = :keyword")
	List<KeywordMember> findByKeyword(@Param("keyword") Keyword keyword);

	@Query("SELECT km FROM KeywordMember km WHERE km.keyword = :keyword AND km.member = :member")
	Optional<KeywordMember> findByKeywordAndMember(@Param("keyword") Keyword keyword, @Param("member") Member member);

	@Modifying
	@Query("UPDATE KeywordMember km SET km.isRead = :isRead WHERE km.keyword = :keyword AND km.member = :member")
	void updateReadStatus(@Param("isRead") boolean isRead, @Param("keyword") Keyword keyword, @Param("member") Member member);

	boolean existsByMemberAndIsReadFalse(Member member);
}