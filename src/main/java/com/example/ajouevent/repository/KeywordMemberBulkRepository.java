package com.example.ajouevent.repository;

import com.example.ajouevent.domain.KeywordMember;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@Repository
public class KeywordMemberBulkRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void saveAll(List<KeywordMember> keywordMembers) {
		String sql = "INSERT INTO keyword_member (keyword_id, member_id) VALUES (?, ?)";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				KeywordMember keywordMember = keywordMembers.get(i);
				ps.setLong(1, keywordMember.getKeyword().getId());
				ps.setLong(2, keywordMember.getMember().getId());
			}

			@Override
			public int getBatchSize() {
				return keywordMembers.size();
			}
		});
	}
}

