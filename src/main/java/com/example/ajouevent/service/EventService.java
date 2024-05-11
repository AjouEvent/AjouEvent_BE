package com.example.ajouevent.service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.ClubEventImage;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Type;
import com.example.ajouevent.dto.EventDetailResponseDto;
import com.example.ajouevent.dto.EventResponseDto;
import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.PostEventDto;
import com.example.ajouevent.dto.PostNotificationDto;
import com.example.ajouevent.dto.SliceResponse;
import com.example.ajouevent.dto.UpdateEventRequest;
import com.example.ajouevent.repository.AlarmRepository;
import com.example.ajouevent.repository.ClubEventImageRepository;
import com.example.ajouevent.repository.EventRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;

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
	private final ClubEventImageRepository clubEventImageRepository;
	private final S3Upload s3Upload;
	private final FileService fileService;

	// 행사, 동아리, 학생회 이벤트와 같은 알림 등록용 메서드
	// Controller의 호출없이 주기적으로 계속 실행
	@Scheduled(fixedRate = 60000)
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
			.createdAt(LocalDateTime.now())
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

	// 게시글 생성 - S3 스프링부트에서 변환
	@Transactional
	public void newEvent(PostEventDto postEventDto, List<MultipartFile> images) {

		List<String> postImages = new ArrayList<>(); // 이미지 URL을 저장할 리스트 생성

		// String presignedUrl = fileService.getS3(); // s3 presigned url 사용

		// images 리스트가 null이 아닌 경우에만 반복 처리
		if (images != null) {
			for (MultipartFile image : images) { // 매개변수로 받은 이미지들을 하나씩 처리
				try {
					String imageUrl = s3Upload.uploadFiles(image, "images"); // 이미지 업로드
					log.info("S3에 올라간 이미지: " + imageUrl); // 로그에 업로드된 이미지 URL 출력
					postImages.add(imageUrl); // 업로드된 이미지 URL을 리스트에 추가
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} else {
			log.info("제공된 이미지가 없습니다.");
		}

		ClubEvent clubEvent = ClubEvent.builder()
			.title(postEventDto.getTitle())
			.content(postEventDto.getContent())
			.url(postEventDto.getUrl())
			.createdAt(LocalDateTime.now())
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

	// 게시글 생성 - S3 프론트에서 변환
	@Transactional
	public void postEvent(PostEventDto postEventDto) {

		ClubEvent clubEvent = ClubEvent.builder()
			.title(postEventDto.getTitle())
			.content(postEventDto.getContent())
			.url(postEventDto.getUrl())
			.createdAt(postEventDto.getEventDateTime())
			.writer(postEventDto.getWriter())
			.subject(postEventDto.getSubject())
			.type(postEventDto.getType())
			.clubEventImageList(new ArrayList<>())
			.build();

		// 프론트엔드에서 받은 이미지 URL 리스트를 처리
		if (postEventDto.getImageUrls() != null) {
			for (String imageUrl : postEventDto.getImageUrls()) {
				ClubEventImage clubEventImage = ClubEventImage.builder()
					.url(imageUrl)
					.clubEvent(clubEvent)
					.build();
				clubEvent.getClubEventImageList().add(clubEventImage);
			}
		}

		eventRepository.save(clubEvent);
	}


	// // 게시글 수정 - 데이터
	// @Transactional
	// public void updateEventData(Long eventId, UpdateEventRequest request) {
	//
	// 	// 수정할 게시글 조회
	// 	ClubEvent clubEvent = eventRepository.findById(eventId)
	// 		.orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));
	//
	// 	// 게시글 내용 수정
	// 	clubEvent.updateEvent(request);
	//
	// 	// 이미지 목록 불러 오기
	// 	List<ClubEventImage> existingImages = clubEvent.getClubEventImageList();
	//
	// 	List<String> existingUrls = existingImages.stream()
	// 		.map(ClubEventImage::getUrl)
	// 		.collect(Collectors.toList());
	//
	// 	existingUrls.forEach(url -> log.info(" 기존 이미지 URL 리스트 : {}", url));
	//
	// 	// 새로운 이미지 URL 리스트
	// 	List<String> newUrls = request.getImageUrls();
	//
	// 	// 삭제할 이미지 엔티티 목록 생성
	// 	List<ClubEventImage> toDeleteImages = existingImages.stream()
	// 		.filter(image -> !newUrls.contains(image.getUrl()))
	// 		.collect(Collectors.toList());
	//
	// 	// S3에서 삭제 및 데이터베이스에서 삭제
	// 	toDeleteImages.forEach(image -> {
	// 		try {
	// 			String splitStr = ".com/";
	// 			String fileName = image.getUrl().substring(image.getUrl().lastIndexOf(splitStr) + splitStr.length());
	// 			fileService.deleteFile(fileName);
	// 			log.info("Deleting image from S3 with fileName: {}", fileName);
	// 		} catch (IOException e) {
	// 			log.error("Failed to delete image from S3: {}", image.getUrl(), e);
	// 		}
	// 	});
	//
	// 	// 삭제할 이미지 URL 리스트 생성
	// 	List<String> toDeleteUrls = existingUrls.stream()
	// 		.filter(url -> !newUrls.contains(url))
	// 		.collect(Collectors.toList());
	//
	// 	toDeleteImages.forEach(image -> log.info("디비에서 삭제하는 이미지 url" + image.getUrl()) );
	// 	// 데이터베이스에서 삭제
	//
	// 	clubEventImageRepository.deleteClubEventImagesByUrls(toDeleteUrls);
	// 	clubEventImageRepository.flush();
	// 	log.info("Deleted {} images from database", toDeleteImages.size());
	//
	// 	// 추가할 이미지 URL 찾기
	// 	List<String> toAddUrls = newUrls.stream()
	// 		.filter(url -> !existingUrls.contains(url))
	// 		.collect(Collectors.toList());
	//
	// 	// S3에 새롭게 추가될 이미지를 ClubEventImage 엔티티로 생성하고 저장
	// 	toAddUrls.forEach(url -> {
	// 		ClubEventImage newImage = ClubEventImage.builder()
	// 			.url(url)
	// 			.clubEvent(clubEvent)
	// 			.build();
	// 		// clubEvent.getClubEventImageList().add(newImage);
	// 		clubEventImageRepository.save(newImage);
	// 		log.info("새로 추가하는 이미지 URL : {}", url);
	// 	});
	//
	// 	newUrls.forEach(url -> log.info(" 새로운 이미지 URL 리스트 : {}", url));
	//
	// 	eventRepository.save(clubEvent);
	// }


	// 게시글 수정 - 데이터 -> 성능 개선
	@Transactional
	public void updateEventData(Long eventId, UpdateEventRequest request) {
		// 수정할 게시글 조회
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));

		// 게시글 내용 수정
		clubEvent.updateEvent(request);

		// 이미지 목록 불러 오기
		List<ClubEventImage> existingImages = clubEvent.getClubEventImageList();
		Set<String> existingUrls = existingImages.stream()
			.map(ClubEventImage::getUrl)
			.collect(Collectors.toSet());

		// 새로운 이미지 URL 리스트
		Set<String> newUrls = new HashSet<>(request.getImageUrls());

		// 변경 필요한 작업 식별
		List<ClubEventImage> toDeleteImages = new ArrayList<>();
		List<String> toAddUrls = new ArrayList<>();

		// 식별 과정 최적화
		existingImages.forEach(image -> {
			if (!newUrls.contains(image.getUrl())) {
				toDeleteImages.add(image);
			}
		});

		newUrls.forEach(url -> {
			if (!existingUrls.contains(url)) {
				toAddUrls.add(url);
			}
		});

		// S3에서 이미지 삭제 및 데이터베이스 삭제 - 비동기 실행
		deleteImagesAsync(toDeleteImages);

		// 새 이미지 추가
		List<ClubEventImage> addedImages = toAddUrls.stream()
			.map(url -> ClubEventImage.builder()
				.url(url)
				.clubEvent(clubEvent)
				.build())
			.collect(Collectors.toList());

		clubEventImageRepository.saveAll(addedImages);

		// 로깅
		logChanges(existingUrls, newUrls);
	}

	// 비동기 삭제 처리
	@Async
	public void deleteImagesAsync(List<ClubEventImage> images) {
		List<String> urlsToDelete = images.stream().map(ClubEventImage::getUrl).collect(Collectors.toList());
		images.forEach(image -> {
			try {
				String fileName = extractFileName(image.getUrl());
				fileService.deleteFile(fileName);
				log.info("Deleting image from S3 with fileName: {}", fileName);
			} catch (IOException e) {
				log.error("Failed to delete image from S3: {}", image.getUrl(), e);
			}
		});
		clubEventImageRepository.deleteClubEventImagesByUrls(urlsToDelete);
		clubEventImageRepository.flush();
	}

	// 파일명 추출
	private String extractFileName(String url) {
		String splitStr = ".com/";
		return url.substring(url.lastIndexOf(splitStr) + splitStr.length());
	}

	// 변경 로깅
	private void logChanges(Set<String> existingUrls, Set<String> newUrls) {
		existingUrls.forEach(url -> log.info("Existing image URL: {}", url));
		newUrls.forEach(url -> log.info("New image URL: {}", url));
	}

	// 게시글 수정 - 이미지
	@Transactional
	public void updateEventImages(Long eventId, List<MultipartFile> images) throws IOException {
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new IllegalArgumentException("Event not found with id: " + eventId));

		// Process and update images if there are any
		List<ClubEventImage> updatedImages = new ArrayList<>();
		if (images != null && !images.isEmpty()) {
			for (MultipartFile image : images) {
				String imageUrl = s3Upload.uploadFiles(image, "images");
				ClubEventImage clubEventImage = ClubEventImage.builder()
					.url(imageUrl)
					.clubEvent(clubEvent)
					.build();
				updatedImages.add(clubEventImage);
			}
			// Remove old images and add updated ones
			clubEvent.getClubEventImageList().clear();
			clubEvent.getClubEventImageList().addAll(updatedImages);
		}

		// Save the updated event
		eventRepository.save(clubEvent);
		log.info("Updated event with ID: " + eventId);
	}

	// 게시글 삭제
	@Transactional
	public void deleteEvent(Long eventId) {
		eventRepository.deleteById(eventId);
	}


	// 글 전체 조회 (동아리, 학생회, 공지사항, 기타)
	@Transactional
	public SliceResponse<EventResponseDto> getEventList(Pageable pageable) {
		Slice<ClubEvent> clubEventSlice = eventRepository.findAll(pageable);

		List<EventResponseDto> eventResponseDtoList = clubEventSlice.getContent().stream()
			.map(EventResponseDto::toDto)
			.collect(Collectors.toList());

		// SliceResponse 생성
		SliceResponse.SortResponse sortResponse = SliceResponse.SortResponse.builder()
			.sorted(pageable.getSort().isSorted())
			.direction(String.valueOf(pageable.getSort().descending()))
			.orderProperty(pageable.getSort().stream().map(Sort.Order::getProperty).findFirst().orElse(null))
			.build();

		return new SliceResponse<>(eventResponseDtoList, clubEventSlice.hasPrevious(), clubEventSlice.hasNext(),
			clubEventSlice.getNumber(), sortResponse);
	}


	@Transactional
	public SliceResponse<EventResponseDto> getEventTypeList(String type, Pageable pageable) {
		// 대소문자를 구분하지 않고 입력 받기 위해 입력된 문자열을 대문자로 변환합니다.
		Type eventType;
		try {
			eventType = Type.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException e) {
			// 유효하지 않은 Type이 입력된 경우, 빈 리스트를 반환합니다.
			return new SliceResponse<>(Collections.emptyList(), false, false, pageable.getPageNumber(), null);
		}

		// Spring Data JPA의 Slice를 사용하여 페이지로 나눠서 결과를 조회합니다.
		Slice<ClubEvent> clubEventSlice = eventRepository.findByType(eventType, pageable);

		// ClubEvent를 EventResponseDto로 변환합니다.
		List<EventResponseDto> eventResponseDtoList = clubEventSlice.getContent().stream()
			.map(EventResponseDto::toDto)
			.collect(Collectors.toList());

		// SliceResponse 생성
		SliceResponse.SortResponse sortResponse = SliceResponse.SortResponse.builder()
			.sorted(pageable.getSort().isSorted())
			.direction(String.valueOf(pageable.getSort().descending()))
			.orderProperty(pageable.getSort().stream().map(Sort.Order::getProperty).findFirst().orElse(null))
			.build();

		// 결과를 Slice로 감싸서 반환합니다.
		return new SliceResponse<>(eventResponseDtoList, clubEventSlice.hasPrevious(), clubEventSlice.hasNext(),
			clubEventSlice.getNumber(), sortResponse);
	}

	// 게시글 상세 조회
	@Transactional
	public EventDetailResponseDto getEventDetail(Long eventId) {
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new NoSuchElementException("Event not found with id: " + eventId));

		return EventDetailResponseDto.toDto(clubEvent);
	}

	public void GoogleAPIClient() throws IOException, GeneralSecurityException {
		/*
		 * 서비스 계정 인증
		 */
		String keyFileName = "credentials.json";
		InputStream keyFile = ResourceUtils.getURL("classpath:" + keyFileName).openStream();
		GoogleCredentials credential = GoogleCredentials.fromStream(keyFile).createScoped(List.of(CalendarScopes.CALENDAR)).createDelegated("calendarmanager@ajouevent.iam.gserviceaccount.com");

		NetHttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

		HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credential);

		Calendar service = new Calendar.Builder(transport, GsonFactory.getDefaultInstance(), requestInitializer).setApplicationName("ajoevent").build();

		String calendarId = "jk2310400@ajou.ac.kr";

		/*
		 * 캘린더 일정 생성
		 */
		Event event = new Event()
			.setSummary("test") // 일정 이름
			.setDescription("teststst"); // 일정 설명

		DateTime startDateTime = new DateTime("2024-05-18T09:00:00-07:00");
		EventDateTime start = new EventDateTime()
			.setDateTime(startDateTime)
			.setTimeZone("Asia/Seoul");
		event.setStart(start);
		DateTime endDateTime = new DateTime("2024-05-19T09:00:00-07:00");
		EventDateTime end = new EventDateTime()
			.setDateTime(endDateTime)
			.setTimeZone("Asia/Seoul");
		event.setEnd(end);


		//이벤트 실행
		event = service.events().insert(calendarId, event).execute();
		System.out.printf("Event created: %s\n", event.getHtmlLink());

	}
}
