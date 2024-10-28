package com.example.ajouevent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.Topic;

@Repository
public interface KeywordRepository extends JpaRepository<Keyword, Long> {
	Optional<Keyword> findBySearchKeyword(String searchKeyword);
	Optional<Keyword> findByEncodedKeyword(String encodedKeyword);

	List<Keyword> findByTopic(Topic topic);
}