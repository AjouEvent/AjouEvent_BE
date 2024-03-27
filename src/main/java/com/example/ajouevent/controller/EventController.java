package com.example.ajouevent.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.ajouevent.dto.UserDTO;
import com.example.ajouevent.service.EventService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/event")
public class EventController {

	private final EventService eventService;
	@PostMapping("/notification")
	public void startEventTime(@RequestBody UserDTO.LoginRequest loginRequest){
		eventService.sendEventNotification(loginRequest);
	}

	@GetMapping("/test")
	public String testGetMethod() {
		return "get";
	}
}
