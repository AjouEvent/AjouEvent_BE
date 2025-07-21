package com.example.ajouevent.domain;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "topic")
public class Topic {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String department;

	@Enumerated(EnumType.STRING)
	@Column(unique = true)
	private Type type;

	@Column
	private String classification;

	@Column
	private String koreanTopic;

	@Column
	private Long koreanOrder;

	@OneToMany(mappedBy = "topic")
	private List<TopicToken> topicTokens;

}
