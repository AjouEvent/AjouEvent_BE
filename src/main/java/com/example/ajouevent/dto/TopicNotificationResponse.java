package com.example.ajouevent.dto;

import java.time.LocalDateTime;

import com.example.ajouevent.domain.PushNotification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TopicNotificationResponse {
	private Long id; // 푸시 알림 ID
	private String title; // 푸시 알림 제목
	private String imageUrl; // 알림 이미지 URL
	private String clickUrl; // 클릭 시 URL
	private boolean isRead; // 읽음 여부
	private LocalDateTime notifiedAt; // 알림 시간
	private String topicName; // Topic의 이름

	public static TopicNotificationResponse toDto(PushNotification pushNotification) {
		return TopicNotificationResponse.builder()
			.id(pushNotification.getId())
			.title(pushNotification.getBody())
			.imageUrl(pushNotification.getImageUrl())
			.clickUrl(pushNotification.getClickUrl())
			.isRead(pushNotification.isRead())
			.notifiedAt(pushNotification.getNotifiedAt())
			.topicName(pushNotification.getTopic().getKoreanTopic()) // Topic 이름
			.build();
	}
}