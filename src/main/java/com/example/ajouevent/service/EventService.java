package com.example.ajouevent.service;

import java.io.IOException;
import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.dto.EventWithKeywordDto;
import com.example.ajouevent.dto.MemberReadStatusDto;
import com.example.ajouevent.logger.WebhookLogger;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.KeywordRepository;
import com.example.ajouevent.repository.TopicRepository;
import com.example.ajouevent.util.SecurityUtil;
import com.example.ajouevent.util.JsonParsingUtil;
import com.example.ajouevent.domain.EventBanner;
import com.example.ajouevent.domain.EventLike;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.dto.EventBannerDto;
import com.example.ajouevent.dto.EventBannerRequest;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.logger.CacheLogger;
import com.example.ajouevent.repository.EventBannerRepository;
import com.example.ajouevent.repository.EventLikeRepository;
import com.example.ajouevent.repository.TopicMemberRepository;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.ClubEventImage;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Type;
import com.example.ajouevent.dto.EventDetailResponseDto;
import com.example.ajouevent.dto.EventResponseDto;
import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.PostEventDto;
import com.example.ajouevent.dto.SliceResponse;
import com.example.ajouevent.dto.UpdateEventRequest;
import com.example.ajouevent.repository.ClubEventImageRepository;
import com.example.ajouevent.repository.EventRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
	private final MemberRepository memberRepository;
	private final EventRepository eventRepository;
	private final ClubEventImageRepository clubEventImageRepository;
	private final S3Upload s3Upload;
	private final FileService fileService;
	private final EventLikeRepository eventLikeRepository;
	private final TopicMemberRepository topicMemberRepository;
	private final EventBannerRepository eventBannerRepository;
	private final KeywordRepository keywordRepository;
	private final KeywordMemberRepository keywordMemberRepository;
	private final JsonParsingUtil jsonParsingUtil;
	private final CacheLogger cacheLogger;
	private final WebhookLogger webhookLogger;
	private final CookieService cookieService;
	private final StringRedisTemplate stringRedisTemplate;
	private final RedisService redisService;

	// 게시글 생성시 기본 좋아요 수 상수 정의(기본 좋아요 수는 0)
	final Long DEFAULT_LIKES_COUNT = 0L;
	final Long DEFAULT_VIEW_COUNT = 0L;
	private final TopicRepository topicRepository;

	// 크롤링한 공지사항 DB에 저장
	@Transactional
	public Long postNotice(NoticeDto noticeDto) {
		Type type = Type.valueOf(noticeDto.getEnglishTopic().toUpperCase());
		log.info("저장하는 타입 : " + type.getEnglishTopic());

		ClubEvent clubEvent = ClubEvent.builder()
			.title(noticeDto.getTitle())
			.content(noticeDto.getContent())
			.createdAt(LocalDateTime.now()) // 크롤링한 공지사항의 게시글 시간은 크롤링하는 당시 시간으로 설정
			.url(noticeDto.getUrl())
			.subject(noticeDto.getKoreanTopic())
			.writer(noticeDto.getDepartment())
			.type(type)
			.likesCount(DEFAULT_LIKES_COUNT)
			.viewCount(DEFAULT_VIEW_COUNT)
			.build();

		log.info("크롤링한 공지사항 원래 url" + noticeDto.getUrl());

		// 기본 default 이미지는 학교 로고
		String image = "https://www.ajou.ac.kr/_res/ajou/kr/img/intro/img-symbol.png";

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

		// 크롤링 후 해당 타입의 캐시 초기화
		jsonParsingUtil.clearCacheForType(noticeDto.getEnglishTopic());

		// 공지사항에 해당하는 토픽을 구독 중인 모든 키워드 찾기
		Topic topic = topicRepository.findByDepartment(noticeDto.getEnglishTopic())
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		// 공지사항에 해당하는 토픽을 구독 중인 모든 TopicMember 조회
		List<TopicMember> topicMembers = topicMemberRepository.findByTopic(topic);

		// 구독자들의 읽음 상태를 '읽지 않음'으로 설정
		for (TopicMember topicMember : topicMembers) {
			topicMember.setRead(false);  // 읽음 상태를 읽지 않음으로 설정
			topicMember.setLastReadAt(LocalDateTime.now());
			topicMemberRepository.save(topicMember);  // 업데이트된 읽음 상태 저장

			// Member의 구독탭 상태를 false로 업데이트
			memberRepository.updateTopicTabReadStatus(topicMember.getMember(), false);
		}

		List<Keyword> keywords = keywordRepository.findByTopic(topic);

		// 키워드를 구독하는 KeywordMember 조회
		for (Keyword keyword : keywords) {
			String koreanKeyword = keyword.getKoreanKeyword();

			// 공지사항의 제목이나 본문에 키워드가 포함되어 있는지 확인
			if (noticeDto.getTitle().contains(koreanKeyword)) {
				// 해당 키워드를 구독 중인 사용자들을 조회
				List<KeywordMember> keywordMembers = keywordMemberRepository.findByKeyword(keyword);

				// 각 구독자의 읽음 상태를 '읽지 않음'으로 설정
				for (KeywordMember keywordMember : keywordMembers) {
					keywordMember.setRead(false);  // 읽음 상태를 읽지 않음으로 설정
					keywordMember.setLastReadAt(LocalDateTime.now());
					keywordMemberRepository.save(keywordMember);

					// Member의 키워드탭 상태를 false로 업데이트
					memberRepository.updateKeywordTabReadStatus(keywordMember.getMember(), false);
				}
			}
		}

		return clubEvent.getEventId();
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
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));
		eventRepository.deleteById(eventId);

		// 게시글 삭제 후 해당 타입의 캐시 초기화
		jsonParsingUtil.clearCacheForType(clubEvent.getType().getEnglishTopic());
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

			List<EventLike> likedEventSlice = eventLikeRepository.findByMember(member);
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

		String cacheKey = type + ":" + pageable.getPageNumber() + ":" + keyword;
		Optional<SliceResponse<EventResponseDto>> cachedData = jsonParsingUtil.getData(cacheKey, new TypeReference<SliceResponse<EventResponseDto>>() {});

		if (cachedData.isPresent()) {
			SliceResponse<EventResponseDto> response = cachedData.get();
			if (principal != null) {
				// 읽음 상태 업데이트: 사용자가 토픽의 공지사항을 조회했으므로 읽음으로 처리
				Member member = memberRepository.findByEmail(principal.getName())
					.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));
				Topic topic = topicRepository.findByDepartment(type)
					.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));
				// 동기적으로 찜 상태를 업데이트
				updateLikeStatusForUser(response.getResult(), principal.getName());
				markTopicAsRead(member, topic);
			}

			// 조회수, 좋아요수를 실시간으로 반영
			updateViewCountAndLikesCountForEvents(response.getResult());

			return response;
		}

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

		for (EventResponseDto dto : eventResponseDtoList) {
			dto.setStar(false);
		}

		// SliceResponse 생성
		SliceResponse.SortResponse sortResponse = SliceResponse.SortResponse.builder()
			.sorted(pageable.getSort().isSorted())
			.direction(String.valueOf(pageable.getSort().descending()))
			.orderProperty(pageable.getSort().stream().map(Sort.Order::getProperty).findFirst().orElse(null))
			.build();

		log.info("조회하는 타입 : " + type);

		SliceResponse<EventResponseDto> response = new SliceResponse<>(eventResponseDtoList, clubEventSlice.hasPrevious(),
			clubEventSlice.hasNext(),
			clubEventSlice.getNumber(), sortResponse);

		// 캐시에는 찜 상태를 전부 false로 설정한 데이터를 저장합니다.
		jsonParsingUtil.saveData(cacheKey, response, 6, TimeUnit.HOURS);

		// 동기적으로 찜 상태를 업데이트
		if (principal != null) {
			updateLikeStatusForUser(response.getResult(), principal.getName());
			// 읽음 상태 업데이트: 사용자가 토픽의 공지사항을 조회했으므로 읽음으로 처리
			Member member = memberRepository.findByEmail(principal.getName())
				.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));
			Topic topic = topicRepository.findByDepartment(type)
				.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

			// 읽음 상태 업데이트
			markTopicAsRead(member, topic);
		}

		// 결과를 Slice로 감싸서 반환합니다.
		return new SliceResponse<>(eventResponseDtoList, clubEventSlice.hasPrevious(), clubEventSlice.hasNext(),
			clubEventSlice.getNumber(), sortResponse);

	}


	// 게시글 상세 조회
	@Transactional
	public EventDetailResponseDto getEventDetail(Long eventId, Principal principal, HttpServletRequest request, HttpServletResponse response) {
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		String userId = SecurityUtil.getCurrentMemberUsernameOrAnonymous();

		EventDetailResponseDto responseDto = EventDetailResponseDto.toDto(clubEvent, false);

		if (isAnonymous(userId)) {
			handleAnonymousUserWithCookieAndRedis(request, response, clubEvent);
		} else {
			handleAuthenticatedUser(userId, clubEvent);
			updateLikeStatusForUser(responseDto, userId);
		}

		return responseDto;
	}

	private boolean isAnonymous(String userId) {
		return "Anonymous".equals(userId);
	}

	private void handleAnonymousUserWithCookieAndRedis(HttpServletRequest request, HttpServletResponse response, ClubEvent clubEvent) {
		String ipAddress = getClientIp(request);
		String userAgent = request.getHeader("User-Agent");
		String currentCookieValue = cookieService.getCookieValue(request, clubEvent);

		// Redis 키 생성
		String redisKey = "ClubEvent_View:" + clubEvent.getEventId() + ":" + ipAddress + ":" + userAgent;

		// 쿠키가 없거나 조회된 적 없는 경우
		if (!cookieService.isAlreadyViewed(currentCookieValue, clubEvent.getEventId())) {
			ResponseCookie newCookie = cookieService.createOrUpdateCookie(currentCookieValue, clubEvent);
			response.addHeader("Set-Cookie", newCookie.toString());

			// 쿠키가 없으면 Redis에서 한 번 더 확인
			if (Boolean.FALSE.equals(stringRedisTemplate.hasKey(redisKey))) {
				stringRedisTemplate.opsForValue().set(redisKey, "0", 86400L, TimeUnit.SECONDS); // TTL과 함께 설정

				// 조회수 증가
				increaseViews(clubEvent);
			}
		}
	}

	private String getClientIp(HttpServletRequest request) {
		String ip = request.getHeader("X-Forwarded-For");
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getHeader("WL-Proxy-Client-IP");
		}
		if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
			ip = request.getRemoteAddr();
		}
		return ip.split(",")[0].trim(); // X-Forwarded-For는 콤마로 구분된 여러 IP를 가질 수 있음
	}

	private void handleAuthenticatedUser(String userId, ClubEvent clubEvent) {
		if (redisService.isFirstIpRequest(userId, clubEvent.getEventId(), clubEvent)) {
			redisService.writeClientRequest(userId, clubEvent.getEventId(), clubEvent);
			increaseViews(clubEvent);
		}
	}

	private void increaseViews(ClubEvent clubEvent){
		String key = "ClubEvent:views:" + clubEvent.getEventId();
		Boolean exist = stringRedisTemplate.opsForValue().setIfAbsent(key, String.valueOf(clubEvent.getViewCount()+1),4L,TimeUnit.MINUTES);
		if(Boolean.FALSE.equals(exist)){
			stringRedisTemplate.opsForValue().increment(key);
			stringRedisTemplate.expire(key,4L,TimeUnit.MINUTES);
		}
	}

	// 사용자가 구독하고 있는 topic 관련 글 조회(로그인 안하면 기본은 AjouNormal)
	@Transactional
	public SliceResponse<EventResponseDto> getSubscribedEvents(Pageable pageable, Principal principal, String keyword) {
		// 사용자가 로그인하지 않은 경우
		if (principal == null) {
			String type = String.valueOf(Type.AJOUNORMAL);
			keyword = keyword == null ? "" : keyword;  // 검색 키워드가 없는 경우 빈 문자열
			return getEventTypeList(type, keyword, pageable, principal);
		}

		String userEmail = principal.getName();
		log.info("사용자 이메일: {}", userEmail);

		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 사용자가 구독하는 모든 토픽 가져오기
		List<TopicMember> subscribedTopicMembers = topicMemberRepository.findByMemberWithTopic(member);

		if (subscribedTopicMembers.isEmpty()) {
			log.info("사용자가 구독하는 토픽이 없습니다.");
		}

		// // 각 TopicMember의 Topic과 Type을 로그로 출력
		// for (TopicMember topicMember : subscribedTopicMembers) {
		// 	Topic topic = topicMember.getTopic();
		// 	if (topic != null) {
		// 		log.info("Topic ID: {}, Type: {}", topic.getId(), topic.getType());
		// 	} else {
		// 		log.warn("TopicMember에 연결된 Topic이 null입니다.");
		// 	}
		// }

		// 토픽 멤버에서 토픽만 추출하여 Type 열거형 리스트로 변환
		List<Type> subscribedTypes = subscribedTopicMembers.stream()
			.map(TopicMember::getTopic)
			.map(Topic::getType)
			.collect(Collectors.toList());

		// // 각 구독하는 토픽을 로그로 출력
		// for (Type type : subscribedTypes) {
		// 	if (type != null) {
		// 		log.info("사용자가 구독하는 토픽: {}", type.getEnglishTopic());
		// 	} else {
		// 		log.warn("null 토픽이 발견되었습니다.");
		// 	}
		// }

		// 검색 기능 추가
		Slice<ClubEvent> clubEventSlice;
		if (keyword != null && !keyword.isEmpty()) {
			// 검색어가 있을 경우, 검색어에 맞는 이벤트만 필터링
			clubEventSlice = eventRepository.findByTypeInAndTitleContaining(subscribedTypes, keyword, pageable);
		} else {
			// 검색어가 없을 경우, 모든 이벤트 조회
			clubEventSlice = eventRepository.findByTypeIn(subscribedTypes, pageable);
		}

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
		if (principal == null || SecurityContextHolder.getContext().getAuthentication() == null) {
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

	// 인기글 조회
	@Transactional
	public List<EventResponseDto> getTopPopularEvents(Principal principal) {
		String cacheKey = "TopPopular";
		Optional<List<EventResponseDto>> cachedData = jsonParsingUtil.getData(cacheKey, new TypeReference<List<EventResponseDto>>() {});
		if (cachedData.isPresent()) {
			List<EventResponseDto> response = cachedData.get();
			if (principal != null) {
				// 동기적으로 찜 상태를 업데이트
				updateLikeStatusForUser(response, principal.getName());
			}
			// 조회수, 좋아요수를 실시간으로 반영
			updateViewCountAndLikesCountForEvents(response);
			return response;
		}

		List<EventResponseDto> eventResponseDtoList = getTop10EventsForCurrentWeek();
		for (EventResponseDto dto : eventResponseDtoList) {
			dto.setStar(false);
		}

		// 캐시에는 찜 상태를 전부 false로 설정한 데이터를 저장합니다.
		jsonParsingUtil.saveData(cacheKey, eventResponseDtoList, 6, TimeUnit.HOURS);

		// 사용자가 로그인한 경우에만 찜한 이벤트 목록을 가져와서 설정합니다.
		if (principal != null) {
			updateLikeStatusForUser(eventResponseDtoList, principal.getName());
		}

		// 이벤트를 이벤트 응답 DTO로 변환하여 반환
		return eventResponseDtoList;
	}

	// 이번주에 생성된 게시글 중 조회수 탑10 게시글 조회 후 DTO 반환
	@Transactional
	public List<EventResponseDto> getTop10EventsForCurrentWeek() {
		LocalDate now = LocalDate.now();
		LocalDate startOfWeek = now.with(DayOfWeek.MONDAY);
		LocalDate endOfWeek = now.with(DayOfWeek.SUNDAY);

		LocalDateTime startOfWeekDateTime = startOfWeek.atStartOfDay();
		LocalDateTime endOfWeekDateTime = endOfWeek.atTime(LocalTime.MAX);

		// 이번주의 이벤트를 조회수 기준으로 정렬하여 가져옴
		List<ClubEvent> clubEventList = eventRepository.findTop10ByCreatedAtBetweenOrderByViewCountDesc(startOfWeekDateTime, endOfWeekDateTime);

		// ClubEvent 목록을 EventResponseDto 목록으로 변환
		return clubEventList.stream()
			.map(EventResponseDto::toDto)
			.collect(Collectors.toList());
	}

	// 홈화면에 들어갈 이벤트 배너 추가
	public void addEventBanner(EventBannerRequest eventBannerRequest) {
		EventBanner eventBanner = EventBanner.builder()
			.imgUrl(eventBannerRequest.getImgUrl())
			.siteUrl(eventBannerRequest.getSiteUrl())
			.bannerOrder(eventBannerRequest.getBannerOrder())
			.startDate(eventBannerRequest.getStartDate())
			.endDate(eventBannerRequest.getEndDate())
			.build();
		eventBannerRepository.save(eventBanner);

		//캐시 초기화
		jsonParsingUtil.clearCache("Banners");
	}

	// 이벤트 배너 삭제
	public void deleteEventBanner(Long eventBannerId) {
		EventBanner eventBanner = eventBannerRepository.findById(eventBannerId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.BANNER_NOT_FOUND));
		eventBannerRepository.delete(eventBanner);

		//캐시 초기화
		jsonParsingUtil.clearCache("Banners");
	}

	// 홈화면에 들어갈 이벤트 배너 불러오기
	public List<EventBannerDto> getAllEventBanners() {
		String cacheKey = "Banners";
		Optional<List<EventBannerDto>> cachedData = jsonParsingUtil.getData(cacheKey, new TypeReference<List<EventBannerDto>>() {});

		if (cachedData.isPresent()) {
			List<EventBannerDto> response = cachedData.get();
			return response;
		}

		List<EventBanner> eventBannerDtoList = eventBannerRepository.findAllByOrderByBannerOrderAsc();

		jsonParsingUtil.saveData(cacheKey, eventBannerDtoList, 6, TimeUnit.HOURS);

		return eventBannerDtoList.stream()
			.map(EventBannerDto::toDto)
			.collect(Collectors.toList());
	}

	// 기간 지난 배너 삭제
	@Scheduled(cron = "0 0 1 * * ?")
	@Transactional
	public void deleteExpiredBanners() {
		LocalDate now = LocalDate.now();
		eventBannerRepository.deleteByEndDateBefore(now);

		//캐시 초기화
		jsonParsingUtil.clearCache("Banners");
	}

	// 랭킹 1시간마다 업데이트
	@Scheduled(cron = "0 0 0/1 * * *")
	@Transactional
	public void refreshTopPopularEvents() {
		String cacheKey = "TopPopular";
		List<EventResponseDto> eventResponseDtoList = getTop10EventsForCurrentWeek();
		for (EventResponseDto dto : eventResponseDtoList) {
			dto.setStar(false);
		}
		jsonParsingUtil.saveData(cacheKey, eventResponseDtoList, 6, TimeUnit.HOURS);
	}

	// EventResponseDtoList에 대한 찜한 상태 업데이트
	private void updateLikeStatusForUser(List<EventResponseDto> eventResponseDtoList, String userEmail) {
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
	}

	// EventDetailResponseDto(상세페이지)에 대한 찜한 상태 업데이트
	public void updateLikeStatusForUser(EventDetailResponseDto eventDetailResponseDto, String userEmail) {
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 사용자가 찜한 게시글 목록 조회
		List<EventLike> likedEventSlice = member.getEventLikeList();
		Map<Long, Boolean> likedEventMap = likedEventSlice.stream()
			.collect(Collectors.toMap(eventLike -> eventLike.getClubEvent().getEventId(), eventLike -> true));

		// 이벤트 DTO에 사용자의 찜 여부 설정
		eventDetailResponseDto.setStar(likedEventMap.getOrDefault(eventDetailResponseDto.getEventId(), false));
	}

	// 이벤트 응답 DTO 목록에 조회수, 좋아요 수 업데이트
	private void updateViewCountAndLikesCountForEvents(List<EventResponseDto> eventResponseDtoList) {
		List<Long> eventIds = eventResponseDtoList.stream()
			.map(EventResponseDto::getEventId)
			.collect(Collectors.toList());

		List<ClubEvent> clubEvents = eventRepository.findAllById(eventIds);

		Map<Long, Long> eventIdToViewCountMap = clubEvents.stream()
			.collect(Collectors.toMap(ClubEvent::getEventId, ClubEvent::getViewCount));

		Map<Long, Long> eventIdToLikesCountMap = clubEvents.stream()
			.collect(Collectors.toMap(ClubEvent::getEventId, ClubEvent::getLikesCount));

		for (EventResponseDto dto : eventResponseDtoList) {
			Long viewCount = eventIdToViewCountMap.get(dto.getEventId());
			dto.setViewCount(viewCount != null ? viewCount : 0);

			Long likesCount = eventIdToLikesCountMap.get(dto.getEventId());
			dto.setLikesCount(likesCount != null ? likesCount : 0);
		}
	}

	// 구독하는 키워드를 포함한 게시글 조회
	@Transactional(readOnly = true)
	public SliceResponse<EventWithKeywordDto> getAllClubEventsBySubscribedKeywords(Principal principal, Pageable pageable) {
		// 사용자가 로그인하지 않은 경우
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		String userEmail = principal.getName();
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		List<KeywordMember> keywordMembers = keywordMemberRepository.findByMemberWithKeywordAndTopic(member);
		List<Keyword> keywords = keywordMembers.stream()
			.map(KeywordMember::getKeyword)
			.toList();

		List<EventWithKeywordDto> eventWithKeywordDtos = new ArrayList<>();
		boolean hasNext = false;

		// 각 키워드에 대해 게시글을 검색하고 결과를 리스트에 추가합니다.
		for (Keyword keyword : keywords) {
			Type type = keyword.getTopic().getType();
			Slice<ClubEvent> clubEventSlice = eventRepository.findByTypeAndTitleContaining(type, keyword.getKoreanKeyword(), pageable);

			// 검색된 게시글을 리스트에 추가합니다.
			List<EventWithKeywordDto> eventWithKeywordDtoList = clubEventSlice.getContent().stream()
				.map(clubEvent -> EventWithKeywordDto.toDto(clubEvent, keyword.getKoreanKeyword()))
				.toList();
			eventWithKeywordDtos.addAll(eventWithKeywordDtoList);

			// Slice의 hasNext 값 업데이트
			hasNext = clubEventSlice.hasNext();
		}

		// 사용자가 찜한 게시글 목록 조회
		List<EventLike> likedEventSlice = member.getEventLikeList();
		Map<Long, Boolean> likedEventMap = likedEventSlice.stream()
			.collect(Collectors.toMap(eventLike -> eventLike.getClubEvent().getEventId(), eventLike -> true));

		// 각 이벤트 DTO에 사용자의 찜 여부 설정
		for (EventWithKeywordDto dto : eventWithKeywordDtos) {
			dto.setStar(likedEventMap.getOrDefault(dto.getEventId(), false));
		}

		// 정렬 및 SliceResponse로 반환
		List<EventWithKeywordDto> sortedDtos = eventWithKeywordDtos.stream()
			.sorted(Comparator.comparing(EventWithKeywordDto::getCreatedAt).reversed())
			.toList();

		SliceResponse<EventWithKeywordDto> response = SliceResponse.<EventWithKeywordDto>builder()
			.result(sortedDtos)
			.hasPrevious(pageable.getPageNumber() > 0)
			.hasNext(hasNext)
			.currentPage(pageable.getPageNumber())
			.sort(SliceResponse.SortResponse.builder()
				.sorted(pageable.getSort().isSorted())
				.direction(pageable.getSort().getOrderFor("createdAt").getDirection().name())
				.orderProperty("createdAt")
				.build())
			.build();

		return response;
	}

	// 단일 키워드 대상 글 조회
	@Transactional
	public SliceResponse<EventWithKeywordDto> getClubEventsByKeyword(String searchKeyword, Principal principal, Pageable pageable) {
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		String userEmail = principal.getName();
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Keyword keyword = keywordRepository.findBySearchKeyword(searchKeyword)
			.orElseThrow(() -> new CustomException(CustomErrorCode.KEYWORD_NOT_FOUND));

		// 사용자가 구독한 해당 키워드의 읽음 상태를 업데이트
		KeywordMember keywordMember = keywordMemberRepository.findByKeywordAndMember(keyword, member)
			.orElseThrow(() -> new CustomException(CustomErrorCode.KEYWORD_NOT_FOUND));
		if (keywordMember.isRead() == false) {
			keywordMember.setRead(true);  // 읽음 상태로 업데이트
			keywordMember.setLastReadAt(LocalDateTime.now());  // 마지막으로 읽은 시간 설정
			keywordMemberRepository.save(keywordMember);  // 업데이트된 읽음 상태 저장
		}


		// 키워드에 해당하는 이벤트 페이징 조회
		Type type = keyword.getTopic().getType();
		Slice<ClubEvent> clubEventSlice = eventRepository.findByTypeAndTitleContaining(type, keyword.getKoreanKeyword(), pageable);

		List<EventWithKeywordDto> eventWithKeywordDtos = clubEventSlice.getContent().stream()
			.map(clubEvent -> EventWithKeywordDto.toDto(clubEvent, keyword.getKoreanKeyword()))
			.toList();

		// 사용자가 찜한 게시글 목록 조회
		List<EventLike> likedEventSlice = member.getEventLikeList();
		Map<Long, Boolean> likedEventMap = likedEventSlice.stream()
			.collect(Collectors.toMap(eventLike -> eventLike.getClubEvent().getEventId(), eventLike -> true));

		// 각 이벤트 DTO에 사용자의 찜 여부 설정
		for (EventWithKeywordDto dto : eventWithKeywordDtos) {
			dto.setStar(likedEventMap.getOrDefault(dto.getEventId(), false));
		}

		// 정렬된 결과 반환
		List<EventWithKeywordDto> sortedDtos = eventWithKeywordDtos.stream()
			.sorted(Comparator.comparing(EventWithKeywordDto::getCreatedAt).reversed())
			.toList();

		// SliceResponse 변환
		SliceResponse<EventWithKeywordDto> response = SliceResponse.<EventWithKeywordDto>builder()
			.result(sortedDtos)
			.hasPrevious(pageable.getPageNumber() > 0)
			.hasNext(clubEventSlice.hasNext())
			.currentPage(pageable.getPageNumber())
			.sort(SliceResponse.SortResponse.builder()
				.sorted(pageable.getSort().isSorted())
				.direction(pageable.getSort().getOrderFor("createdAt") != null
					? pageable.getSort().getOrderFor("createdAt").getDirection().name()
					: "DESC")
				.orderProperty("createdAt")
				.build())
			.build();

		return response;
	}

	// 특정 사용자의 구독 토픽에 대한 읽음 상태를 업데이트하는 메서드
	@Transactional
	public void markTopicAsRead(Member member, Topic topic) {
		TopicMember topicMember = topicMemberRepository.findByMemberAndTopic(member, topic)
			.orElse(TopicMember.builder()
				.member(member)
				.topic(topic)
				.isRead(true)
				.lastReadAt(LocalDateTime.now())  // 읽음 처리 시, 현재 시간으로 설정
				.build());

		topicMember.setRead(true);  // 읽음 상태로 설정
		topicMember.setLastReadAt(LocalDateTime.now());  // 마지막으로 읽은 시각 업데이트
		topicMemberRepository.save(topicMember);  // 변경사항 저장
	}

	@Transactional(readOnly = true)
	public MemberReadStatusDto getMemberReadStatus(Principal principal) {
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		Member member = memberRepository.findByEmail(principal.getName()).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));
		return MemberReadStatusDto.builder()
			.isTopicTabRead(member.getIsTopicTabRead())
			.isKeywordTabRead(member.getIsKeywordTabRead())
			.build();
	}

	// 구독 알림 읽음 상태를 관리하기 위한 로직
	@Transactional
	public void updateTopicTabReadStatus(Principal principal) {
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		Member member = memberRepository.findByEmail(principal.getName()).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));
		// // 멤버가 구독한 모든 토픽의 읽음 상태 조회
		// List<TopicMember> topicMembers = topicMemberRepository.findByMember(member);
		// boolean allTopicsRead = topicMembers.stream().allMatch(TopicMember::isRead);
		//
		// // 모든 구독한 토픽이 읽음 상태일 경우만 구독 알림의 상태를 true로 설정
		// if (allTopicsRead) {
		// 	log.info("토픽 알림 탭 읽음상태 업데이트 - true");
		// 	memberRepository.updateTopicTabReadStatus(member, true);
		// }
		memberRepository.updateTopicTabReadStatus(member, true);
	}

	// 키워드 알림 읽음 상태를 관리하기 위한 로직
	@Transactional
	public void updateKeywordTabReadStatus(Principal principal) {
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		Member member = memberRepository.findByEmail(principal.getName()).orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// // 멤버가 구독한 모든 키워드의 읽음 상태 조회
		// List<KeywordMember> keywordMembers = keywordMemberRepository.findByMember(member);
		//
		// // 각 키워드의 읽음 상태를 로그로 출력
		// keywordMembers.forEach(keywordMember ->
		// 	log.info("Keyword: {}, isRead: {}",
		// 		keywordMember.getKeyword().getKoreanKeyword(),
		// 		keywordMember.getIsRead())
		// );
		//
		//
		// boolean allKeywordsRead = keywordMembers.stream().allMatch(KeywordMember::getIsRead);
		//
		// // 모든 구독한 키워드가 읽음 상태일 경우만 키워드 알림의 상태를 true로 설정
		// if (allKeywordsRead) {
		// 	log.info("키워드 알림 탭 읽음상태 업데이트 - true");
		// 	memberRepository.updateKeywordTabReadStatus(member, true);
		// }
		memberRepository.updateKeywordTabReadStatus(member, true);
	}

	@Transactional(readOnly = true)
	public boolean isDuplicateNotice(String topic, String title, String url) {
		Type type;
		try {
			type = Type.valueOf(topic.toUpperCase());
		} catch (IllegalArgumentException e) {
			String errorMessage = String.format("잘못된 공지사항 Type 값: '%s' - 존재하지 않는 Enum 값입니다.", topic);
			webhookLogger.log(errorMessage);
			throw new CustomException(CustomErrorCode.INVALID_TYPE);
		}

		List<ClubEvent> recentEvents = eventRepository.findTop10ByTypeOrderByCreatedAtDesc(type);
		return recentEvents.stream()
			.anyMatch(event -> event.getTitle().equals(title) && event.getUrl().equals(url));
	}
}
