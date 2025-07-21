package com.example.ajouevent.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "keyword")
public class Keyword extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column
	private String encodedKeyword;

	@Column
	private String koreanKeyword;

	@Column
	String searchKeyword;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "topic_id")
	private Topic topic;

	@Builder
	private Keyword(String encodedKeyword, String koreanKeyword, String searchKeyword, Topic topic) {
		this.encodedKeyword = encodedKeyword;
		this.koreanKeyword = koreanKeyword;
		this.searchKeyword = searchKeyword;
		this.topic = topic;
	}

	public static Keyword create(String encodedKeyword, String koreanKeyword, String searchKeyword, Topic topic) {
		return Keyword.builder()
			.encodedKeyword(encodedKeyword)
			.koreanKeyword(koreanKeyword)
			.searchKeyword(searchKeyword)
			.topic(topic)
			.build();
	}
}
