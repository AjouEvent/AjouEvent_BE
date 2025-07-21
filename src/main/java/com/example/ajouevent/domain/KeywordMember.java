package com.example.ajouevent.domain;

import java.time.LocalDateTime;

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
@Table(name = "keyword_member")
public class KeywordMember extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "keyword_id")
	private Keyword keyword;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@Column(nullable = false, columnDefinition = "TINYINT(1)")
	private boolean isRead;  // 해당 키워드의 새 공지사항 읽음 여부

	@Column(nullable = false)
	private LocalDateTime lastReadAt;  // 마지막으로 읽은 시각

	@Builder
	private KeywordMember(Keyword keyword, Member member, boolean isRead, LocalDateTime lastReadAt) {
		this.keyword = keyword;
		this.member = member;
		this.isRead = isRead;
		this.lastReadAt = lastReadAt;
	}

	public static KeywordMember create(Keyword keyword, Member member) {
		return KeywordMember.builder()
			.keyword(keyword)
			.member(member)
			.isRead(false) // 구독 후, 기본 읽음 상태는 false
			.lastReadAt(LocalDateTime.now())
			.build();
	}

	public void markAsRead() {
		this.isRead = true;
		this.lastReadAt = LocalDateTime.now();
	}

	public void markAsUnread() {
		this.isRead = false;
		this.lastReadAt = LocalDateTime.now();
	}
}
