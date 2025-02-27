package com.example.ajouevent.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.NotificationType;
import com.example.ajouevent.domain.PushCluster;
import com.example.ajouevent.domain.PushNotification;

@Repository
public interface PushNotificationRepository extends JpaRepository<PushNotification, Long> {
	Optional<PushNotification> findByMemberAndId(Member member, Long id);

	List<PushNotification> findAllByPushCluster(PushCluster pushCluster);

	Slice<PushNotification> findByMemberAndNotificationType(Member member, NotificationType notificationType, Pageable pageable);
	int countByMemberAndIsReadFalse(Member member);
}
