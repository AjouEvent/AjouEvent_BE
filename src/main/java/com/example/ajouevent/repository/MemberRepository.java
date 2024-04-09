package com.example.ajouevent.repository;

import java.util.Optional;

import com.example.ajouevent.domain.Member;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MemberRepository extends JpaRepository<Member, Long> {

	Optional<Member> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByEmailAndPassword(String email, String password);

	void deleteByEmail(String email);

}
