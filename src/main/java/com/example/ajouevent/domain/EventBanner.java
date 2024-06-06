package com.example.ajouevent.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class EventBanner {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long eventBannerId;

	@Column(nullable = false)
	private Long imgOrder;

	@OneToOne
	@JoinColumn(name = "clubEvent_id")
	private ClubEvent clubEvent;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

}
