package com.example.ajouevent.service;

import java.io.IOException;
import java.security.Principal;
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
	public void createNotification(PostNotificationDTO postNotificationDTO, Principal principal) {
		String email = principal.getName();
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

	@Transactional
	public void postEvent(PostEventDTO postEventDto, List<MultipartFile> images) {

		List<String> postImages = new ArrayList<>(); // 이미지 URL을 저장할 리스트 생성

		for (MultipartFile image : images) { // 매개변수로 받은 이미지들을 하나씩 처리
			try {
				String imageUrl = s3Upload.uploadFiles(image, "images"); // 이미지 업로드
				log.info("S3에 올라간 이미지: " + imageUrl); // 로그에 업로드된 이미지 URL 출력
				postImages.add(imageUrl); // 업로드된 이미지 URL을 리스트에 추가
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		ClubEvent clubEvent = ClubEvent.builder()
			.title(postEventDto.getTitle())
			.content(postEventDto.getContent())
			.writer(postEventDto.getWriter())
			.subject(postEventDto.getSubject())
			.type(postEventDto.getType())
			.clubEventImageList(new ArrayList<>())
			.build();

		// 각 업로드된 이미지의 URL을 사용하여 ClubEventImage를 생성하고, ClubEvent와 연관시킵니다.
		for (String postImage : postImages) {
			log.info("S3에 올라간 이미지: " + postImage);
			ClubEventImage clubEventImage = ClubEventImage.builder()
				.url(postImage)
				.clubEvent(clubEvent)
				.build();
			clubEvent.getClubEventImageList().add(clubEventImage);
		}


		eventRepository.save(clubEvent);

	}

	@Transactional
	public List<EventResponseDTO> getEventList() {
		List<ClubEvent> clubEventEntities = eventRepository.findAll();
		List<EventResponseDTO> eventResponseDTOList = new ArrayList<>();

		for (ClubEvent clubEvent : clubEventEntities) {
			EventResponseDTO eventResponseDTO = EventResponseDTO.toDto(clubEvent);
			eventResponseDTOList.add(eventResponseDTO);
		}

		return eventResponseDTOList;
	}
}
