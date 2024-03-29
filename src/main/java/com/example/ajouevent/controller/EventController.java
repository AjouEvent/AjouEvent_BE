package com.example.ajouevent.controller;

import com.example.ajouevent.dto.PostEventDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.ajouevent.dto.UserDTO;
import com.example.ajouevent.service.EventService;

import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

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

//	@PostMapping("/new")
//	public void createEvent(@RequestPart MultipartFile image, @RequestPart PostEventDto postEventDto){
//
//	}
}
