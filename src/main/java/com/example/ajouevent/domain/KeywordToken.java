package com.example.ajouevent.domain;

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
@Table(name = "keyword_token")
public class KeywordToken extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "keyword_id")
	private Keyword keyword;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "token_id")
	private Token token;

	@Builder
	private KeywordToken(Keyword keyword, Token token) {
		this.keyword = keyword;
		this.token = token;
	}

	public static KeywordToken create(Keyword keyword, Token token) {
		return KeywordToken.builder()
			.keyword(keyword)
			.token(token)
			.build();
	}
}
