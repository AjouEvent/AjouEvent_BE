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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
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
