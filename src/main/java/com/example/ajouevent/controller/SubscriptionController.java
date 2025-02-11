package com.example.ajouevent.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.dto.TabReadStatusResponse;
import com.example.ajouevent.service.SubscriptionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/subscriptions")
@Slf4j
public class SubscriptionController {
	private final SubscriptionService subscriptionService;

	@GetMapping("/isSubscribedTabRead")
	public TabReadStatusResponse isSubscribedTabRead() {
		return subscriptionService.isSubscribedTabRead();
	}

}
