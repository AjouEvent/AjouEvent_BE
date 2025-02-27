package com.example.ajouevent.repository;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.PushNotification;

@Repository
public class PushNotificationBulkRepository {

	@Autowired
	private JdbcTemplate jdbcTemplate;


	public void saveAll(List<PushNotification> notifications) {
		String sql = "INSERT INTO push_notification " +
			"(push_cluster_id, member_id, topic_id, keyword_id, notification_type, title, body, image_url, click_url, is_read, notified_at, clicked_at) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				PushNotification notification = notifications.get(i);
				ps.setLong(1, notification.getPushCluster() != null ? notification.getPushCluster().getId() : null); // push_cluster_id
				ps.setLong(2, notification.getMember().getId()); // member_id
				ps.setObject(3, notification.getTopic() != null ? notification.getTopic().getId() : null); // topic_id
				ps.setObject(4, notification.getKeyword() != null ? notification.getKeyword().getId() : null); // keyword_id
				ps.setString(5, notification.getNotificationType().name()); // notification_type
				ps.setString(6, notification.getTitle()); // title
				ps.setString(7, notification.getBody()); // body
				ps.setString(8, notification.getImageUrl()); // image_url
				ps.setString(9, notification.getClickUrl()); // click_url
				ps.setBoolean(10, notification.isRead()); // is_read
				ps.setTimestamp(11, Timestamp.valueOf(notification.getNotifiedAt())); // notified_at
				ps.setTimestamp(12, notification.getClickedAt() != null ? Timestamp.valueOf(notification.getClickedAt()) : null); // clicked_at
			}

			@Override
			public int getBatchSize() {
				return notifications.size();
			}
		});
	}

	public void updateReadStatus(List<PushNotification> notifications) {
		String sql = "UPDATE push_notification " +
			"SET is_read = TRUE, clicked_at = ? " +
			"WHERE id = ?";

		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			@Override
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				PushNotification notification = notifications.get(i);
				ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now())); // 현재 시간으로 클릭 시간 설정
				ps.setLong(2, notification.getId()); // 업데이트할 푸시 알림 ID
			}

			@Override
			public int getBatchSize() {
				return notifications.size();
			}
		});
	}
}