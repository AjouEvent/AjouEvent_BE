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
@Table(name = "event_like_table")
public class EventLike extends BaseTimeEntity {

	@Id // pk 지정
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long eventLikeId;

	@ManyToOne
	@JoinColumn(name = "club_event_id")
	private ClubEvent clubEvent;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@Builder
	private EventLike(ClubEvent clubEvent, Member member) {
		this.clubEvent = clubEvent;
		this.member = member;
	}

	public static EventLike create(ClubEvent clubEvent, Member member) {
		return EventLike.builder()
			.clubEvent(clubEvent)
			.member(member)
			.build();
	}

}