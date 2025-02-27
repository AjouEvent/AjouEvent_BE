package com.example.ajouevent.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.PushCluster;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class PushClusterBulkRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

	public void updateAll(List<PushCluster> pushClusters) {
		String sql = "UPDATE push_cluster SET received_count = ?, clicked_count = ? WHERE id = ?";


		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				PushCluster pushCluster = pushClusters.get(i);

				ps.setInt(1, pushCluster.getReceivedCount()); // received_count
				ps.setInt(2, pushCluster.getClickedCount()); // clicked_count
				ps.setLong(3, pushCluster.getId()); // id
			}

			@Override
			public int getBatchSize() {
				return pushClusters.size();
			}
		});

		// 엔티티 분리
		pushClusters.forEach(entityManager::detach);
	}
}