package com.example.ajouevent.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventBannerRequest {
	private Long eventId;
	private Long imgOrder;
	private LocalDate startDate;
	private LocalDate endDate;
}
