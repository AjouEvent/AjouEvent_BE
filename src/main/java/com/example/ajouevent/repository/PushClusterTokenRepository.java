package com.example.ajouevent.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.PushCluster;
import com.example.ajouevent.domain.PushClusterToken;

@Repository
public interface PushClusterTokenRepository extends JpaRepository<PushClusterToken, Long> {

	@Query("SELECT pct FROM PushClusterToken pct JOIN FETCH pct.token t WHERE pct.pushCluster = :pushCluster")
	List<PushClusterToken> findAllByPushClusterWithToken(@Param("pushCluster") PushCluster pushCluster);

	@Query("SELECT t FROM PushClusterToken t JOIN FETCH t.token tk JOIN FETCH tk.member WHERE t.pushCluster = :pushCluster")
	List<PushClusterToken> findAllByPushClusterWithTokenAndMember(@Param("pushCluster") PushCluster pushCluster);
}
