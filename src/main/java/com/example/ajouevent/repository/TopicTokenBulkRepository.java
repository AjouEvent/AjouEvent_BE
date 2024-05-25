package com.example.ajouevent.repository;

import com.example.ajouevent.domain.TopicToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class TopicTokenBulkRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void saveAll(List<TopicToken> topicTokens) {
		String sql = "INSERT INTO topic_token (topic_id, token_id) VALUES (?, ?)";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				TopicToken topicToken = topicTokens.get(i);
				ps.setLong(1, topicToken.getTopic().getId());
				ps.setLong(2, topicToken.getToken().getId());
			}

			@Override
			public int getBatchSize() {
				return topicTokens.size();
			}
		});
	}
}
