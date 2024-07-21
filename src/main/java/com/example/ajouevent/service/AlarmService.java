package com.example.ajouevent.service;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Type;
import com.example.ajouevent.dto.PostNotificationDto;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.logger.AlarmLogger;
import com.example.ajouevent.repository.AlarmRepository;
import com.example.ajouevent.repository.MemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlarmService {
	private final AlarmRepository alarmRepository;
	private final MemberRepository memberRepository;
	private final AlarmLogger alarmLogger;
	private final FCMService fcmService;

	@Scheduled(fixedRate = 60000)
	@Transactional
	public void sendAlarm() {
		LocalDateTime now = LocalDateTime.now();
		int nowHour = now.getHour();
		int nowMinute = now.getMinute();


		// 1. 현재 시간에 해당하는 알림을 다 찾음
		// 2. 이 알림을 등록한 사용자들에게 전부 알림 전송 -> 비동기로

		List<Alarm> alarms = alarmRepository.findAll();

		for (Alarm alarm: alarms) {
			LocalDate alarmDate = alarm.getAlarmDateTime().toLocalDate();
			LocalTime alarmTime = alarm.getAlarmDateTime().toLocalTime();
			alarmLogger.log("알람 날짜: " + alarmDate);
			alarmLogger.log("알람 시간: " + alarmTime);

			if (alarm.getAlarmDateTime().toLocalDate() == alarmDate && alarm.getAlarmDateTime().getHour() == nowHour && alarm.getAlarmDateTime().getMinute() == nowMinute) {
				fcmService.sendAlarm(alarm.getMember().getEmail(), alarm);
			}
		}
	}

	@Transactional
	public void createAlarm(PostNotificationDto postNotificationDTO, Principal principal) {
		String userEmail = principal.getName();
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Type type = Type.valueOf(postNotificationDTO.getType().getEnglishTopic().toUpperCase());

		Alarm alarm = Alarm.builder()
			.title(postNotificationDTO.getTitle())
			.content(postNotificationDTO.getContent())
			.writer(postNotificationDTO.getWriter())
			.alarmDateTime(postNotificationDTO.getAlarmDateTime())
			.subject(postNotificationDTO.getSubject())
			.type(type)
			.member(member).build();
		alarmRepository.save(alarm);
	}
}
