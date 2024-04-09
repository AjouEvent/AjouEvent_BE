package com.example.ajouevent.service;

import static org.junit.jupiter.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Type;
import com.example.ajouevent.dto.PostNotificationDTO;
import com.example.ajouevent.repository.AlarmRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.service.EventService;

@SpringBootTest
public class EventServiceTest {

	@Autowired
	private EventService eventService;

	@Autowired
	private AlarmRepository alarmRepository;

	@Autowired
	private MemberRepository memberRepository;

	@Test
	@Transactional
	public void testSendNotifications() {
		// Given
		Member member1 = Member.builder().email("user1@example.com").build();
		memberRepository.save(member1);

		List<Alarm> alarms = new ArrayList<>();

		// Create 100 alarms for the same user
		for (int i = 0; i < 100; i++) {
			LocalDateTime alarmTime = LocalDateTime.now().plusSeconds(10); // 각 알람은 5분 간격으로 설정됩니다.
			Alarm alarm = Alarm.builder()
				.alarmDateTime(alarmTime)
				.title("Alarm " + i)
				.content("Content for Alarm " + i)
				.writer("Writer " + i)
				.subject("Subject " + i)
				.member(member1)
				.build();
			alarms.add(alarm);
		}

		alarmRepository.saveAll(alarms);

		// When
		eventService.sendEventNotification();

		// Then
		// Add verification here to verify that notifications are sent to respective users
	}

	// @Test
	// @Transactional
	// public void testCreateNotification() {
	// 	// Given
	// 	PostNotificationDTO notificationDTO = PostNotificationDTO.builder()
	// 		.title("Test Notification")
	// 		.content("Test content")
	// 		.writer("Test writer")
	// 		.subject("Test subject")
	// 		.alarmDateTime(LocalDateTime.now())
	// 		.type(Type.Student)
	// 		.build();
	//
	// 	// When
	// 	eventService.createNotification(notificationDTO);
	//
	// 	// Then
	// 	// Add verification here to verify that notification is created successfully
	// }
}
