package com.example.ajouevent.domain;

import com.google.firebase.database.annotations.NotNull;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@Table(name = "event_like_table")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EventLike {

	@Id // pk 지정
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int eventLikeId;

	// 찜한 이벤트 카테고리 -> 동아리 / 학생회 / 크롤링 공지사항 / 기타
	@Column
	@NotNull
	private int categoryNumber;


	@ManyToOne
	@JoinColumn(name = "club_event_id")
	private ClubEvent clubEvent;

	// @Column
	// @NotNull
	// private int eventId; // 게시글 ID

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

}