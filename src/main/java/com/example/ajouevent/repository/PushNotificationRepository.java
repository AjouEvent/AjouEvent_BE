package com.example.ajouevent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.NotificationType;
import com.example.ajouevent.domain.PushCluster;
import com.example.ajouevent.domain.PushNotification;
import com.example.ajouevent.dto.UnreadNotificationCountDto;

@Repository
public interface PushNotificationRepository extends JpaRepository<PushNotification, Long> {
	Optional<PushNotification> findByMemberAndId(Member member, Long id);

	List<PushNotification> findAllByPushCluster(PushCluster pushCluster);

	Slice<PushNotification> findByMemberAndNotificationType(Member member, NotificationType notificationType, Pageable pageable);
	int countByMemberAndIsReadFalse(Member member);

	@Query("SELECT new com.example.ajouevent.dto.UnreadNotificationCountDto(" +
		"tm.member.id, CAST(COALESCE(COUNT(pn), 0) AS long)) " +  // 🔹 COUNT 결과를 long으로 변환
		"FROM TopicMember tm " +
		"LEFT JOIN PushNotification pn ON pn.member.id = tm.member.id AND pn.isRead = false " +
		"WHERE tm.topic.koreanTopic = :koreanTopic " +
		"GROUP BY tm.member.id")
	List<UnreadNotificationCountDto> countUnreadNotificationsForTopic(@Param("koreanTopic") String koreanTopic);

	@Query("SELECT new com.example.ajouevent.dto.UnreadNotificationCountDto(" +
		"km.member.id, CAST(COALESCE(COUNT(pn), 0) AS long)) " +  // 🔹 COUNT 결과를 long으로 변환
		"FROM KeywordMember km " +
		"LEFT JOIN PushNotification pn ON pn.member.id = km.member.id AND pn.isRead = false " +
		"WHERE km.keyword.encodedKeyword = :encodedKeyword " +
		"GROUP BY km.member.id")
	List<UnreadNotificationCountDto> countUnreadNotificationsForKeyword(@Param("encodedKeyword") String encodedKeyword);
}
