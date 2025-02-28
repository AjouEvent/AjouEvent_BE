package com.example.ajouevent.repository;

import java.util.Optional;

import com.example.ajouevent.domain.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByEmail(String email);

	@Query("SELECT m FROM Member m " +
		"LEFT JOIN FETCH m.topicMembers tm " + "WHERE m.email = :email")
	Optional<Member> findByEmailWithSubscriptions(@Param("email") String email);

	@Query("SELECT m FROM Member m LEFT JOIN FETCH m.tokens WHERE m.email = :email")
	Optional<Member> findByEmailWithTokens(@Param("email") String email);

	@Query("SELECT m FROM Member m LEFT JOIN FETCH m.tokens t WHERE m.email = :email AND t.isDeleted = false")
	Optional<Member> findByEmailWithValidTokens(@Param("email") String email);

	boolean existsByEmailAndName(@Param("email") String email, @Param("name") String name);

	boolean existsByEmail(String email);

	boolean existsByEmailAndPassword(String email, String password);

	void deleteByEmail(String email);


	Member findMemberByEmailAndPhone(String email, String phone);

	@Modifying
	@Query("UPDATE Member m SET m.isTopicTabRead = :isRead WHERE m = :member")
	void updateTopicTabReadStatus(@Param("member") Member member, @Param("isRead") boolean isRead);

	@Modifying
	@Query("UPDATE Member m SET m.isKeywordTabRead = :isRead WHERE m = :member")
	void updateKeywordTabReadStatus(@Param("member") Member member, @Param("isRead") boolean isRead);
}
