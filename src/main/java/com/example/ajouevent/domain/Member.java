package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "member")
public class Member extends BaseTimeEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column
	private String name;

	@Column
	private String password;

	@Column
	private String major;

	@Column
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role = Role.USER;

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST,
		CascadeType.REMOVE}, orphanRemoval = true)
	private List<Token> tokens;

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
	@ToString.Exclude
	private List<Alarm> alarmList;

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST,
		CascadeType.REMOVE}, orphanRemoval = true)
	private List<EventLike> eventLikeList;

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST,
		CascadeType.REMOVE}, orphanRemoval = true)
	private List<TopicMember> topicMembers;

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST,
		CascadeType.REMOVE}, orphanRemoval = true)
	private List<KeywordMember> keywordMembers;

	@Builder
	private Member(Long id, String email, String name) {
		this.id = id;
		this.email = email;
		this.name = name;
	}

	public static Member register(String email, String name) {
		return Member.builder()
			.email(email)
			.name(name)
			.build();
	}

	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
	}

	public void changeMajor(String major) {
		if (major != null) this.major = major;
	}

	public void updateProfile(String name, String major, String phone) {
		if (name != null) this.name = name;
		if (major != null) this.major = major;
		if (phone != null) this.phone = phone;
	}
}
