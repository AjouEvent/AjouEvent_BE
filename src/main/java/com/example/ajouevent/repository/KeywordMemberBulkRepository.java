package com.example.ajouevent.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.KeywordMember;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class KeywordMemberBulkRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

	public void updateKeywordMembers(List<KeywordMember> keywordMembers) {
		String sql = "UPDATE keyword_member SET is_read = ?, last_read_at = ? WHERE id = ?";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				KeywordMember keywordMember = keywordMembers.get(i);
				ps.setBoolean(1, keywordMember.isRead());
				ps.setTimestamp(2, Timestamp.valueOf(keywordMember.getLastReadAt()));
				ps.setLong(3, keywordMember.getId());
			}

			@Override
			public int getBatchSize() {
				return keywordMembers.size();
			}
		});

		// 엔티티 분리
		keywordMembers.forEach(entityManager::detach);
	}
}