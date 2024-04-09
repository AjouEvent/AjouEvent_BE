package com.example.ajouevent.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.ajouevent.S3Upload;
import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.ClubEventImage;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.dto.PostEventDTO;
import com.example.ajouevent.dto.PostNotificationDTO;
import com.example.ajouevent.repository.AlarmRepository;
import com.example.ajouevent.repository.EventRepository;
import com.example.ajouevent.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
	private final FCMService fcmService;
	private final AlarmRepository alarmRepository;
	private final MemberRepository memberRepository;
	private final EventRepository eventRepository;
	private final S3Upload s3Upload;

	@Scheduled(fixedRate = 10000)
	@Transactional
	public void sendEventNotification() {
		LocalDateTime now = LocalDateTime.now();
		int nowHour = now.getHour();
		int nowMinute = now.getMinute();
		log.info("현재 시간:" + now);
		log.info("현재 시간(시간, 분): " + nowHour + "시" + nowMinute + "분");


		// 1. 현재 시간에 해당하는 알림을 다 찾음
		// 2. 이 알림을 등록한 사용자들에게 전부 알림 전송 -> 비동기로

		List<Alarm> alarms = alarmRepository.findAll();

		for (Alarm alarm: alarms) {
			if (alarm.getAlarmDateTime().getHour() == nowHour && alarm.getAlarmDateTime().getMinute() == nowMinute) {
				fcmService.sendEventNotification(alarm.getMember().getEmail(), alarm);
			}
		}

	}

	@Transactional
	public void createNotification(PostNotificationDTO postNotificationDTO) {
		String email = "test1@example.com";

		Member member = memberRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email));

		Alarm alarm = Alarm.builder()
			.title(postNotificationDTO.getTitle())
			.content(postNotificationDTO.getContent())
			.writer(postNotificationDTO.getWriter())
			.alarmDateTime(postNotificationDTO.getAlarmDateTime())
			.subject(postNotificationDTO.getSubject())
			.member(member).build();

		alarmRepository.save(alarm);
	}

}
