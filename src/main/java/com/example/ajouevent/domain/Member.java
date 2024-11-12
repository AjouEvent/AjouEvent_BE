package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
public class Member {
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

	@Column(nullable = false)
	private Boolean isSubscribedTabRead;

	@Column(nullable = false)
	private Boolean isTopicTabRead;

	@Column(nullable = false)
	private Boolean isKeywordTabRead;

	@Builder
	public Member(Long id, String email, String name, String password, String major, String phone, Boolean isSubscribedTabRead, Boolean isTopicTabRead, Boolean isKeywordTabRead) {
		this.id = id;
		this.email = email;
		this.name = name;
		this.password = password;
		this.major = major;
		this.phone = phone;
		this.isSubscribedTabRead = isSubscribedTabRead;
		this.isTopicTabRead = isTopicTabRead;
		this.isKeywordTabRead = isKeywordTabRead;
	}
}
