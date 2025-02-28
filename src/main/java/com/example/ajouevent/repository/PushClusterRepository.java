package com.example.ajouevent.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.PushCluster;

@Repository
public interface PushClusterRepository extends JpaRepository<PushCluster, Long> {

}
