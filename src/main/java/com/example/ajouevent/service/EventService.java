package com.example.ajouevent.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.ajouevent.S3Upload;
import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.ClubEventImage;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Type;
import com.example.ajouevent.dto.EventResponseDto;
import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.PostEventDto;
import com.example.ajouevent.dto.PostNotificationDto;
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


	// 행사, 동아리, 학생회 이벤트와 같은 알림 등록용 메서드
	// Controller의 호출없이 주기적으로 계속 실행
	@Scheduled(fixedRate = 10000)
	@Transactional
	public void sendEventNotification() {
		LocalDateTime now = LocalDateTime.now();
		int nowHour = now.getHour();
		int nowMinute = now.getMinute();


		// 1. 현재 시간에 해당하는 알림을 다 찾음
		// 2. 이 알림을 등록한 사용자들에게 전부 알림 전송 -> 비동기로

		List<Alarm> alarms = alarmRepository.findAll();

		for (Alarm alarm: alarms) {
			if (alarm.getAlarmDateTime().getHour() == nowHour && alarm.getAlarmDateTime().getMinute() == nowMinute) {
				fcmService.sendEventNotification(alarm.getMember().getEmail(), alarm);
			}
		}
	}


	// 크롤링한 공지사항 DB에 저장
	@Transactional
	public void postNotice(NoticeDto noticeDto) {
		Type type = Type.valueOf(noticeDto.getEnglishTopic().toUpperCase());
		log.info("저장하는 타입 : " + type.getEnglishTopic());


		// log.info("저장하는 타입1 : " + stringType);
		// log.info("저장하는 타입2 : " + Type.valueOf(noticeDto.getEnglishTopic()));
		// log.info("저장하는 타입3 : " + Type.AJOUNORMAL.getEnglishTopic());

		ClubEvent clubEvent = ClubEvent.builder()
			.title(noticeDto.getTitle())
			.content(noticeDto.getContent())
			.url(noticeDto.getUrl())
			.type(type)
			.build();

		log.info("크롤링한 공지사항 원래 url" + noticeDto.getUrl());


		// 기본 default 이미지는 학교 로고
		String image = "https://ajou-event-bucket.s3.ap-northeast-2.amazonaws.com/static/1e7b1dc2-ae1b-4254-ba38-d1a0e7cfa00c.20240307_170436.jpg";

		if (noticeDto.getImages() == null || noticeDto.getImages().isEmpty()) {
			log.info("images 리스트가 비어있습니다.");
			// images 리스트가 null 이거나 비어있을 경우, 기본 이미지 리스트를 생성하고 설정
			List<String> defaultImages = new ArrayList<>();
			defaultImages.add(image);
			noticeDto.setImages(defaultImages);
		}

		// -> payload에서 parsing에서 바로 가져올 수 있으면 좋음
		List<ClubEventImage> clubEventImageList = new ArrayList<>();
		for (String imageUrl : noticeDto.getImages()) {
			ClubEventImage clubEventImage = ClubEventImage.builder()
				.url(imageUrl)
				.clubEvent(clubEvent)
				.build();
			clubEventImageList.add(clubEventImage);
		}

		clubEvent.setClubEventImageList(clubEventImageList);


		// 각 업로드된 이미지의 URL을 사용하여 ClubEventImage를 생성하고, ClubEvent와 연관시킵니다.


		// 이미지 URL을 첫 번째 이미지로 설정
		image = String.valueOf(noticeDto.getImages().get(0));

		log.info("공지사항에서 크롤링한 이미지: " + image);

		// ClubEventImage clubEventImage = ClubEventImage.builder()
		// 	.clubEvent(clubEvent)
		// 	.build();
		//
		// clubEvent.getClubEventImageList().add(clubEventImage);

		eventRepository.save(clubEvent);

	}

	@Transactional
	public void createNotification(PostNotificationDto postNotificationDTO, Principal principal) {
		String email = principal.getName();
		Member member = memberRepository.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("해당 이메일을 가진 사용자를 찾을 수 없습니다: " + email));

		Type type = Type.valueOf(postNotificationDTO.getType().getEnglishTopic().toUpperCase());
		log.info("저장하는 타입 : " + type.getEnglishTopic());

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

	@Transactional
	public void postEvent(PostEventDto postEventDto, List<MultipartFile> images) {

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
	public List<EventResponseDto> getEventList() {
		List<ClubEvent> clubEventEntities = eventRepository.findAll();
		List<EventResponseDto> eventResponseDtoList = new ArrayList<>();

		for (ClubEvent clubEvent : clubEventEntities) {
			EventResponseDto eventResponseDTO = EventResponseDto.toDto(clubEvent);
			eventResponseDtoList.add(eventResponseDTO);
		}

		return eventResponseDtoList;
	}

	@Transactional
	public List<EventResponseDto> getEventTypeList(String type) {
		// 대소문자를 구분하지 않고 입력 받기 위해 입력된 문자열을 대문자로 변환합니다.

		// 입력된 문자열이 유효한 Type인지 확인하고, 유효한 경우 해당 Type으로 변환합니다.
		Type eventType;
		try {
			eventType = Type.valueOf(type);
		} catch (IllegalArgumentException e) {
			// 유효하지 않은 Type이 입력된 경우, 빈 리스트를 반환합니다.
			return Collections.emptyList();
		}

		// 유효한 Type에 해당하는 ClubEvent 리스트를 가져옵니다.
		List<ClubEvent> clubEventEntities = eventRepository.findByType(eventType);
		List<EventResponseDto> eventResponseDtoList = new ArrayList<>();

		for (ClubEvent clubEvent : clubEventEntities) {
			EventResponseDto eventResponseDTO = EventResponseDto.toDto(clubEvent);
			eventResponseDtoList.add(eventResponseDTO);
		}

		return eventResponseDtoList;
	}

}
