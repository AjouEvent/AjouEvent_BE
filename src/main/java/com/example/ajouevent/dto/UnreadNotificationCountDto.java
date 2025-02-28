package com.example.ajouevent.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UnreadNotificationCountDto {
	private Long memberId;
	private Long unreadNotificationCount;

	public UnreadNotificationCountDto(Number memberId, Number unreadNotificationCount) {
		this.memberId = memberId.longValue();
		this.unreadNotificationCount = unreadNotificationCount.longValue();
	}
}