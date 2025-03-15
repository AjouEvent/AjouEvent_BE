package com.example.ajouevent.domain;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Banner {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long bannerId;

	@Column(nullable = false)
	private Long bannerOrder;

	@Column(nullable = false)
	private String imgUrl;

	@Column(nullable = false)
	private String siteUrl;

	@Column(nullable = false)
	private LocalDate startDate;

	@Column(nullable = false)
	private LocalDate endDate;

}
