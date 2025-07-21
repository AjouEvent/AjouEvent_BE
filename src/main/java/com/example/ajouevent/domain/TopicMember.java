package com.example.ajouevent.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "topic_member")
public class TopicMember extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "topic_id")
	private Topic topic;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	private boolean isRead;

	@Column(nullable = false)
	private LocalDateTime lastReadAt;

	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	private boolean receiveNotification;

	@Builder
	private TopicMember(Member member, Topic topic, boolean isRead,
		LocalDateTime lastReadAt, boolean receiveNotification) {
		this.member = member;
		this.topic = topic;
		this.isRead = isRead;
		this.lastReadAt = lastReadAt;
		this.receiveNotification = receiveNotification;
	}

	public static TopicMember create(Member member, Topic topic) {
		return TopicMember.builder()
			.member(member)
			.topic(topic)
			.isRead(false)
			.lastReadAt(LocalDateTime.now())
			.receiveNotification(true)
			.build();
	}

	/** 알림을 읽지 않은 상태로 변경 */
	public void markAsUnread() {
		this.isRead = false;
		this.lastReadAt = LocalDateTime.now();
	}

	/** 알림을 읽은 상태로 변경 */
	public void markAsRead() {
		this.isRead = true;
		this.lastReadAt = LocalDateTime.now();
	}

	/** 알림 수신 여부 변경 */
	public void changeReceiveNotification(boolean value) {
		this.receiveNotification = value;
	}
}
