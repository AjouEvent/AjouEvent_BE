package com.example.ajouevent.dto;

import lombok.Data;

@Data
public class NotificationPreferenceRequest {
	private String topic;
	private boolean receiveNotification;
}
