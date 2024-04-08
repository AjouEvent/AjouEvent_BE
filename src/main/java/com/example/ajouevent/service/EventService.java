package com.example.ajouevent.service;

import com.example.ajouevent.S3Upload;
import com.example.ajouevent.dto.PostEventDto;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.ajouevent.dto.UserDTO;
import com.example.ajouevent.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@RequiredArgsConstructor
public class EventService {
	private final FCMService fcmService;
	private final UserRepository userRepository;
	private final S3Upload s3Upload;

	@Scheduled(fixedRate = 1000)
	public void sendEventNotification(UserDTO.LoginRequest loginRequest) {
		String email = loginRequest.getEmail();
		fcmService.sendEventNotification(email);
	}

	public void createEvent(MultipartFile image, PostEventDto postEventDto) {

	}
}
