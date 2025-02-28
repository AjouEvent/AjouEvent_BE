package com.example.ajouevent.repository;

import com.example.ajouevent.domain.PushClusterToken;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class PushClusterTokenBulkRepository {

	private static final Logger log = LoggerFactory.getLogger(PushClusterTokenBulkRepository.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void saveAll(List<PushClusterToken> clusterTokens) {
		log.info("PushClusterTokenBulkRepository saveAll");

		String sql = "INSERT INTO push_cluster_token (push_cluster_id, token_id, job_status, request_time, processed_time) VALUES (?, ?, ?, ?, ?)";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				PushClusterToken clusterToken = clusterTokens.get(i);
				ps.setLong(1, clusterToken.getPushCluster().getId());
				ps.setLong(2, clusterToken.getToken().getId());
				ps.setString(3, clusterToken.getJobStatus().name());
				ps.setTimestamp(4, java.sql.Timestamp.valueOf(clusterToken.getRequestTime()));
				ps.setTimestamp(5, clusterToken.getProcessedTime() != null ?
					java.sql.Timestamp.valueOf(clusterToken.getProcessedTime()) : null); // processedTime 추가
			}

			@Override
			public int getBatchSize() {
				return clusterTokens.size();
			}
		});
	}

	@PersistenceContext
	private EntityManager entityManager;

	public void updateAll(List<PushClusterToken> clusterTokens) {
		String sql = "UPDATE push_cluster_token " +
			"SET job_status = ?, processed_time = ?, push_cluster_id = ?, request_time = ?, token_id = ? WHERE id = ?";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				PushClusterToken token = clusterTokens.get(i);
				ps.setString(1, token.getJobStatus().name());
				ps.setTimestamp(2, java.sql.Timestamp.valueOf(token.getProcessedTime()));
				ps.setLong(3, token.getPushCluster().getId()); // push_cluster_id
				ps.setTimestamp(4, java.sql.Timestamp.valueOf(token.getRequestTime())); // request_time
				ps.setLong(5, token.getToken().getId()); // token_id
				ps.setLong(6, token.getId()); // id
			}

			@Override
			public int getBatchSize() {
				return clusterTokens.size();
			}
		});

		// 엔티티 분리
		clusterTokens.forEach(entityManager::detach);
	}
}