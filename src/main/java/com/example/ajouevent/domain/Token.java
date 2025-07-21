package com.example.ajouevent.domain;

import java.time.LocalDate;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "token")
public class Token extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(unique = true)
	private String tokenValue;

	@Column(nullable = false)
	private LocalDate expirationDate;

	@OneToMany(mappedBy = "token")
	private List<TopicToken> topicTokens;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@Column(nullable = false)
	private boolean isDeleted = false;

	@Builder
	private Token(String tokenValue, LocalDate expirationDate, boolean isDeleted, Member member) {
		this.tokenValue = tokenValue;
		this.expirationDate = expirationDate;
		this.isDeleted = isDeleted;
		this.member = member;
	}

	public static Token create(String tokenValue, Member member, int validWeeks) {
		return Token.builder()
			.tokenValue(tokenValue)
			.member(member)
			.expirationDate(LocalDate.now().plusWeeks(validWeeks))
			.isDeleted(false)
			.build();
	}

	public void markAsDeleted() {
		this.isDeleted = true;
	}

	public void extendExpiration(int weeks) {
		this.expirationDate = LocalDate.now().plusWeeks(weeks);
	}
}
