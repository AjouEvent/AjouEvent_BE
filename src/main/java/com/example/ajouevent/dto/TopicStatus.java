package com.example.ajouevent.dto;

import com.example.ajouevent.domain.Topic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TopicStatus {
	private Long id;
	private String topic;
	private boolean subscribed;

	public TopicStatus(Topic topic, boolean subscribed) {
		this.id = topic.getId();
		this.topic = topic.getDepartment();
		this.subscribed = subscribed;
	}
}