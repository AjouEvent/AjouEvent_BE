package com.example.ajouevent.repository;

import com.example.ajouevent.domain.TopicMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class TopicMemberBulkRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void saveAll(List<TopicMember> topicMembers) {
		String sql = "INSERT INTO topic_member (topic_id, member_id) VALUES (?, ?)";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				TopicMember topicMember = topicMembers.get(i);
				ps.setLong(1, topicMember.getTopic().getId());
				ps.setLong(2, topicMember.getMember().getId());
			}

			@Override
			public int getBatchSize() {
				return topicMembers.size();
			}
		});
	}
}
