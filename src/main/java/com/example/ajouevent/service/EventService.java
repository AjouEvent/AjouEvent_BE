package com.example.ajouevent.service;

import java.io.IOException;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.example.ajouevent.domain.EventLike;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.exception.UserNotFoundException;
import com.example.ajouevent.logger.AlarmLogger;
import com.example.ajouevent.repository.EventLikeRepository;
import com.example.ajouevent.repository.TopicMemberRepository;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
	private final EventLikeRepository eventLikeRepository;
	private final TopicMemberRepository topicMemberRepository;
	private final AlarmLogger alarmLogger;

	// 게시글 생성시 기본 좋아요 수 상수 정의(기본 좋아요 수는 0)
	final Long DEFAULT_LIKES_COUNT = 0L;
	final Long DEFAULT_VIEW_COUNT = 0L;

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
			LocalDate alarmDate = alarm.getAlarmDateTime().toLocalDate();
			LocalTime alarmTime = alarm.getAlarmDateTime().toLocalTime();
			alarmLogger.log("알람 날짜: " + alarmDate);
			alarmLogger.log("알람 시간: " + alarmTime);

			if (alarm.getAlarmDateTime().toLocalDate() == alarmDate && alarm.getAlarmDateTime().getHour() == nowHour && alarm.getAlarmDateTime().getMinute() == nowMinute) {
				fcmService.sendEventNotification(alarm.getMember().getEmail(), alarm);
			}
		}
	}


	// 크롤링한 공지사항 DB에 저장
	@Transactional
	public Long postNotice(NoticeDto noticeDto) {
		Type type = Type.valueOf(noticeDto.getEnglishTopic().toUpperCase());
		log.info("저장하는 타입 : " + type.getEnglishTopic());

		ClubEvent clubEvent = ClubEvent.builder()
			.title(noticeDto.getTitle())
			.content(noticeDto.getContent())
			.createdAt(noticeDto.getDate())
			.url(noticeDto.getUrl())
			.subject(noticeDto.getKoreanTopic())
			.writer(noticeDto.getDepartment())
			.type(type)
			.likesCount(DEFAULT_LIKES_COUNT)
			.viewCount(DEFAULT_VIEW_COUNT)
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

		eventRepository.save(clubEvent);

		return clubEvent.getEventId();
	}

	@Transactional
	public void createNotification(PostNotificationDto postNotificationDTO, Principal principal) {
		String userEmail = principal.getName();
		// 사용자 조회
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

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
					throw new CustomException(CustomErrorCode.IMAGE_UPLOAD_FAILED);
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
			.likesCount(DEFAULT_LIKES_COUNT)
			.viewCount(DEFAULT_VIEW_COUNT)
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
			.likesCount(DEFAULT_LIKES_COUNT)
			.viewCount(DEFAULT_VIEW_COUNT)
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
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

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
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

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
	public SliceResponse<EventResponseDto> getEventList(Pageable pageable, String keyword, Principal principal) {
		Slice<ClubEvent> clubEventSlice = eventRepository.findAllByTitleContaining(keyword, pageable);

		// 조회된 ClubEvent 목록을 이벤트 응답 DTO 목록으로 매핑합니다.
		List<EventResponseDto> eventResponseDtoList = clubEventSlice.getContent().stream()
			.map(EventResponseDto::toDto)
			.collect(Collectors.toList());

		for (EventResponseDto dto : eventResponseDtoList) {
			dto.setStar(false);
		}

		// SliceResponse 생성
		SliceResponse.SortResponse sortResponse = SliceResponse.SortResponse.builder()
			.sorted(pageable.getSort().isSorted())
			.direction(String.valueOf(pageable.getSort().descending()))
			.orderProperty(pageable.getSort().stream().map(Sort.Order::getProperty).findFirst().orElse(null))
			.build();

		// 사용자가 로그인한 경우에만 찜한 이벤트 목록을 가져와서 설정합니다.
		if (principal != null) {
			String userEmail = principal.getName();
			Member member = memberRepository.findByEmail(userEmail)
				.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

			Slice<EventLike> likedEventSlice = eventLikeRepository.findByMember(member);
			Map<Long, Boolean> likedEventMap = likedEventSlice.stream()
				.collect(Collectors.toMap(eventLike -> eventLike.getClubEvent().getEventId(), eventLike -> true));

			// 각 이벤트 DTO에 사용자의 찜 여부 설정
			for (EventResponseDto dto : eventResponseDtoList) {
				dto.setStar(likedEventMap.getOrDefault(dto.getEventId(), false));
			}
		}

		return new SliceResponse<>(eventResponseDtoList, clubEventSlice.hasPrevious(), clubEventSlice.hasNext(),
			clubEventSlice.getNumber(), sortResponse);
	}

	// 글 타입별 조회 (동아리, 학생회, 공지사항, 기타)
	@Transactional
	public SliceResponse<EventResponseDto> getEventTypeList(String type, String keyword, Pageable pageable, Principal principal) {
		// 대소문자를 구분하지 않고 입력 받기 위해 입력된 문자열을 대문자로 변환합니다.

		Type eventType;
		try {
			eventType = Type.valueOf(type.toUpperCase());
		} catch (IllegalArgumentException e) {
			// 유효하지 않은 Type이 입력된 경우, 빈 리스트를 반환합니다.
			return new SliceResponse<>(Collections.emptyList(), false, false, pageable.getPageNumber(), null);
		}

		// Spring Data JPA의 Slice를 사용하여 페이지로 나눠서 결과를 조회합니다.
		Slice<ClubEvent> clubEventSlice = eventRepository.findByTypeAndTitleContaining(eventType, keyword, pageable);

		// 조회된 ClubEvent 목록을 이벤트 응답 DTO 목록으로 매핑합니다.
		List<EventResponseDto> eventResponseDtoList = clubEventSlice.getContent().stream()
			.map(EventResponseDto::toDto)
			.collect(Collectors.toList());

		// 사용자가 로그인한 경우에만 찜한 이벤트 목록을 가져와서 설정합니다.
		if (principal != null) {
			log.info("유저 Email" + principal.getName());
			String userEmail = principal.getName();
			Member member = memberRepository.findByEmail(userEmail)
				.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

			// 사용자가 찜한 게시글 목록 조회
			List<EventLike> likedEventSlice = member.getEventLikeList();
			Map<Long, Boolean> likedEventMap = likedEventSlice.stream()
				.collect(Collectors.toMap(eventLike -> eventLike.getClubEvent().getEventId(), eventLike -> true));

			// 각 이벤트 DTO에 사용자의 찜 여부 설정
			for (EventResponseDto dto : eventResponseDtoList) {
				dto.setStar(likedEventMap.getOrDefault(dto.getEventId(), false));
			}
		} else {
			for (EventResponseDto dto : eventResponseDtoList) {
				dto.setStar(false);
			}
		}

		// SliceResponse 생성
		SliceResponse.SortResponse sortResponse = SliceResponse.SortResponse.builder()
			.sorted(pageable.getSort().isSorted())
			.direction(String.valueOf(pageable.getSort().descending()))
			.orderProperty(pageable.getSort().stream().map(Sort.Order::getProperty).findFirst().orElse(null))
			.build();

		log.info("조회하는 타입 : " + type);

		// 결과를 Slice로 감싸서 반환합니다.
		return new SliceResponse<>(eventResponseDtoList, clubEventSlice.hasPrevious(), clubEventSlice.hasNext(),
			clubEventSlice.getNumber(), sortResponse);

	}

	// 게시글 상세 조회
	@Transactional
	public EventDetailResponseDto getEventDetail(Long eventId, Principal principal) {
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		// 조회수 증가
		clubEvent.increaseViewCount();

		if (principal != null) {
			String userEmail = principal.getName(); // 현재 로그인한 사용자의 이메일 가져오기

			// 사용자 조회
			Member member = memberRepository.findByEmail(userEmail)
				.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

			boolean isLiked = eventLikeRepository.existsByMemberAndClubEvent(member, clubEvent);
			return EventDetailResponseDto.toDto(clubEvent, isLiked);
		}

		boolean isLiked = false;
		return EventDetailResponseDto.toDto(clubEvent, isLiked);
	}

	// 사용자가 구독하고 있는 topic 관련 글 조회(로그인 안하면 기본은 AjouNormal)
	@Transactional
	public SliceResponse<EventResponseDto> getSubscribedEvents(Pageable pageable, Principal principal) {
		// 사용자가 로그인하지 않은 경우
		if (principal == null) {
			String type = String.valueOf(Type.AJOUNORMAL);
			String keyword = "";
			return getEventTypeList(type, keyword, pageable, principal);
		}

		String userEmail = principal.getName();
		log.info("사용자 이메일: {}", userEmail);

		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 사용자가 구독하는 모든 토픽 가져오기
		List<TopicMember> subscribedTopicMembers = topicMemberRepository.findByMember(member);

		if (subscribedTopicMembers.isEmpty()) {
			log.info("사용자가 구독하는 토픽이 없습니다.");
		}

		// 각 TopicMember의 Topic과 Type을 로그로 출력
		for (TopicMember topicMember : subscribedTopicMembers) {
			Topic topic = topicMember.getTopic();
			if (topic != null) {
				log.info("Topic ID: {}, Type: {}", topic.getId(), topic.getType());
			} else {
				log.warn("TopicMember에 연결된 Topic이 null입니다.");
			}
		}

		// 토픽 멤버에서 토픽만 추출하여 Type 열거형 리스트로 변환
		List<Type> subscribedTypes = subscribedTopicMembers.stream()
			.map(TopicMember::getTopic)
			.map(Topic::getType)
			.collect(Collectors.toList());

		// 각 구독하는 토픽을 로그로 출력
		for (Type type : subscribedTypes) {
			if (type != null) {
				log.info("사용자가 구독하는 토픽: {}", type.getEnglishTopic());
			} else {
				log.warn("null 토픽이 발견되었습니다.");
			}
		}

		// 변환된 Type 열거형 리스트를 사용하여 이벤트를 조회
		Slice<ClubEvent> clubEventSlice = eventRepository.findByTypeIn(subscribedTypes, pageable);

		// 사용자가 찜한 게시글 목록 조회
		List<EventLike> likedEventSlice = member.getEventLikeList();
		Map<Long, Boolean> likedEventMap = likedEventSlice.stream()
			.collect(Collectors.toMap(eventLike -> eventLike.getClubEvent().getEventId(), eventLike -> true));

		// 이벤트를 이벤트 응답 DTO로 변환하여 반환
		List<EventResponseDto> eventResponseDtoList = clubEventSlice.getContent().stream()
			.map(EventResponseDto::toDto)
			.collect(Collectors.toList());

		// 각 이벤트 DTO에 사용자의 찜 여부 설정
		for (EventResponseDto dto : eventResponseDtoList) {
			dto.setStar(likedEventMap.getOrDefault(dto.getEventId(), false));
		}

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

	// 게시글 찜하기
	@Transactional
	public ResponseEntity<ResponseDto> likeEvent(Long eventId, Principal principal) {
		// 사용자가 로그인하지 않은 경우
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		String userEmail = principal.getName(); // 현재 로그인한 사용자의 이메일 가져오기

		// 이벤트 조회
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		// 사용자 조회
		Member member = memberRepository.findByEmail(userEmail).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 이미 찜한 이벤튼지 확인
		if (eventLikeRepository.existsByMemberAndClubEvent(member, clubEvent)) {
			return ResponseEntity.ok().body(ResponseDto.builder()
				.successStatus(HttpStatus.OK)
				.successContent("이미 찜한 이벤트입니다.")
				.build()
			);
		}

		// 이벤트를 사용자의 찜 목록에 추가
		EventLike eventLike = EventLike.builder()
			.clubEvent(clubEvent)
			.member(member)
			.build();

		// 게시글의 좋아요 수 증가
		clubEvent.incrementLikes();

		eventLikeRepository.save(eventLike);

		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.CREATED)
			.successContent("게시글을 찜했습니다.")
			.build()
		);

	}

	// 게시글 찜 취소 하기
	@Transactional
	public ResponseEntity<ResponseDto> cancelLikeEvent(Long eventId, Principal principal) {
		// 사용자가 로그인하지 않은 경우
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		String userEmail = principal.getName(); // 현재 로그인한 사용자의 이메일 가져오기

		// 이벤트 조회
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		// 사용자 조회
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 찜한 이벤트인지 확인
		EventLike eventLike = eventLikeRepository.findByClubEventAndMember(clubEvent, member).orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_LIKED));
		if (eventLike == null) {
			throw new CustomException(CustomErrorCode.EVENT_NOT_LIKED);
		}

		// 게시글의 저장수 감소
		clubEvent.decreaseLikes();

		// 이벤트 찜 취소
		eventLikeRepository.delete(eventLike);

		return ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("게시글 찜하기를 취소했습니다.")
			.build()
		);
	}

	// 유저의 찜한 이벤트 목록 조회
	@Transactional
	public SliceResponse<EventResponseDto> getLikedEvents(String type, String keyword, Pageable pageable, Principal principal) {
		// 사용자가 로그인하지 않은 경우
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		String userEmail = principal.getName(); // 현재 로그인한 사용자의 이메일 가져오기

		// 사용자 조회
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// Fetch Join을 사용하여 ClubEvent와 관련된 엔티티를 한 번에 가져옵니다.
		// Slice<EventLike> likedEventSlice = eventLikeRepository.findByMemberWithClubEvent(member, pageable);


		// 사용자가 찜한 EventLike 엔티티들을 가져옵니다.
		// List<EventLike> likedEvents = eventLikeRepository.findByMember(member);
		List<EventLike> likedEvents = eventLikeRepository.findByMemberWithClubEvent(member);

		// EventLike 엔티티에서 ClubEvent의 ID 목록을 추출합니다.
		List<Long> eventIds = likedEvents.stream()
			.map(eventLike -> eventLike.getClubEvent().getEventId())
			.collect(Collectors.toList());

		// ClubEvent 엔티티들을 페이징하여 가져옵니다.
		Slice<ClubEvent> clubEventSlice = eventRepository.findByEventIds(eventIds, pageable);

		List<EventResponseDto> eventResponseDtoList = clubEventSlice.getContent().stream()
			.filter(event -> {
				// 타입 조건 평가
				boolean matchesType;
				if (type == null || type.isEmpty()) {
					matchesType = true; // type이 비어 있으면 모든 타입과 일치하도록 설정
				} else {
					matchesType = event.getType().name().equalsIgnoreCase(type); // 타입이 일치하는지 확인
				}

				// 키워드 조건 평가
				boolean matchesKeyword;
				if (keyword == null || keyword.isEmpty()) {
					matchesKeyword = true; // keyword가 비어 있으면 모든 제목과 일치하도록 설정
				} else {
					matchesKeyword = event.getTitle().contains(keyword); // 제목에 키워드가 포함되어 있는지 확인
				}

				// 두 조건이 모두 참이면 필터링 통과
				return matchesType && matchesKeyword;
			})
			.map(event -> {
				EventResponseDto dto = EventResponseDto.toDto(event);
				dto.setStar(true);
				return dto;
			})
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

}
