package com.example.ajouevent.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Token;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Repository
public class TokenBulkRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@PersistenceContext
	private EntityManager entityManager;

	public void updateTokens(List<Token> tokens) {
		String sql = "UPDATE token SET is_deleted = ? WHERE id = ?";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Token token = tokens.get(i);
				ps.setBoolean(1, token.isDeleted()); // is_deleted 값 설정
				ps.setLong(2, token.getId());        // token의 ID 설정
			}

			@Override
			public int getBatchSize() {
				return tokens.size();
			}
		});

		// 엔티티 분리
		tokens.forEach(entityManager::detach);
	}
}