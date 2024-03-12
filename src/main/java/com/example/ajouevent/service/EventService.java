package com.example.ajouevent.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.ajouevent.dto.UserDTO;
import com.example.ajouevent.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventService {
	private final FCMService fcmService;
	private final UserRepository userRepository;

	@Scheduled(fixedRate = 1000)
	public void sendEventNotification(UserDTO.LoginRequest loginRequest) {
		String email = loginRequest.getEmail();
		fcmService.sendEventNotification(email);
	}
}
