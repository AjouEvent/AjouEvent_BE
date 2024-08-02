package com.example.ajouevent.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.KeywordToken;
@Repository
public class KeywordTokenBulkRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void saveAll(List<KeywordToken> keywordTokens) {
		String sql = "INSERT INTO keyword_token (keyword_id, token_id) VALUES (?, ?)";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				KeywordToken keywordToken = keywordTokens.get(i);
				ps.setLong(1, keywordToken.getKeyword().getId());
				ps.setLong(2, keywordToken.getToken().getId());
			}

			@Override
			public int getBatchSize() {
				return keywordTokens.size();
			}
		});
	}
}