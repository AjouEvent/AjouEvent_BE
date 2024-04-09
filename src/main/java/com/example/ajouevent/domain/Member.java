package com.example.ajouevent.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@NoArgsConstructor
@Entity
public class Member {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	private String password;

	@Column
	private String major;

	@Column
	private String phone;

	@OneToMany(mappedBy = "member", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.REMOVE}, orphanRemoval = true)
	@ToString.Exclude
	private List<Alarm> alarmList;

	@Builder
	public Member(Long id, String email, String password) {
		this.id = id;
		this.email = email;
		this.password = password;
	}
}
