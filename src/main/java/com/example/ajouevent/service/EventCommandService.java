package com.example.ajouevent.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.ClubEventImage;
import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.domain.Type;
import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.PostEventDto;
import com.example.ajouevent.dto.UpdateEventRequest;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.logger.WebhookLogger;
import com.example.ajouevent.repository.ClubEventImageRepository;
import com.example.ajouevent.repository.EventRepository;
import com.example.ajouevent.repository.KeywordMemberBulkRepository;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.KeywordRepository;
import com.example.ajouevent.repository.TopicMemberBulkRepository;
import com.example.ajouevent.repository.TopicMemberRepository;
import com.example.ajouevent.repository.TopicRepository;
import com.example.ajouevent.util.JsonParsingUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventCommandService {
	private final EventRepository eventRepository;
	private final ClubEventImageRepository clubEventImageRepository;
	private final S3Upload s3Upload;
	private final FileService fileService;
	private final TopicMemberRepository topicMemberRepository;
	private final KeywordRepository keywordRepository;
	private final KeywordMemberRepository keywordMemberRepository;
	private final JsonParsingUtil jsonParsingUtil;
	private final WebhookLogger webhookLogger;
	private final CookieService cookieService;
	private final StringRedisTemplate stringRedisTemplate;
	private final RedisService redisService;

	// 게시글 생성시 기본 좋아요 수 상수 정의(기본 좋아요 수는 0)
	final Long DEFAULT_LIKES_COUNT = 0L;
	final Long DEFAULT_VIEW_COUNT = 0L;
	private final TopicRepository topicRepository;
	private final TopicMemberBulkRepository topicMemberBulkRepository;
	private final KeywordMemberBulkRepository keywordMemberBulkRepository;

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
		}

		topicMemberBulkRepository.updateTopicMembers(topicMembers);

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
				}
				keywordMemberBulkRepository.updateKeywordMembers(keywordMembers);
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
