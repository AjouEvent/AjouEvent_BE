package com.example.ajouevent.repository;

import java.util.Optional;

import com.example.ajouevent.domain.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import io.lettuce.core.dynamic.annotation.Param;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	@Query("SELECT m FROM Member m " +
		"LEFT JOIN FETCH m.topicMembers tm " + "WHERE m.email = :email")
	Optional<Member> findByEmailWithSubscriptions(@Param("email") String email);

	Optional<Member> findByEmail(String email);

	boolean existsByEmail(String email);

}
