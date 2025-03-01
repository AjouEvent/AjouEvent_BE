package com.example.ajouevent.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.TopicMember;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class TopicMemberBulkRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

	public void updateTopicMembers(List<TopicMember> topicMembers) {
		String sql = "UPDATE topic_member SET is_read = ?, last_read_at = ? WHERE id = ?";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				TopicMember topicMember = topicMembers.get(i);
				ps.setBoolean(1, topicMember.isRead());
				ps.setTimestamp(2, Timestamp.valueOf(topicMember.getLastReadAt()));
				ps.setLong(3, topicMember.getId());
			}

			@Override
			public int getBatchSize() {
				return topicMembers.size();
			}
		});

		// 엔티티 분리
		topicMembers.forEach(entityManager::detach);
	}
}