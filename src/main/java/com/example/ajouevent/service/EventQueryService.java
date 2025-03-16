package com.example.ajouevent.service;

import java.security.Principal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.EventLike;
import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.domain.Type;
import com.example.ajouevent.dto.EventDetailResponseDto;
import com.example.ajouevent.dto.EventResponseDto;
import com.example.ajouevent.dto.EventWithKeywordDto;
import com.example.ajouevent.dto.SliceResponse;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.EventRepository;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.KeywordRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TopicMemberRepository;
import com.example.ajouevent.util.JsonParsingUtil;
import com.example.ajouevent.util.SecurityUtil;
import com.fasterxml.jackson.core.type.TypeReference;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventQueryService {
	private final EventLikeService eventLikeService;
	private final EventRepository eventRepository;
	private final MemberRepository memberRepository;
	private final TopicMemberRepository topicMemberRepository;
	private final KeywordRepository keywordRepository;
	private final KeywordMemberRepository keywordMemberRepository;
	private final JsonParsingUtil jsonParsingUtil;

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
			Map<Long, Boolean> likedEventMap = eventLikeService.getLikedEventMap(member);

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
				// 동기적으로 찜 상태를 업데이트
				updateLikeStatusForUser(response.getResult(), principal.getName());
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
		}

		// 결과를 Slice로 감싸서 반환합니다.
		return new SliceResponse<>(eventResponseDtoList, clubEventSlice.hasPrevious(), clubEventSlice.hasNext(),
			clubEventSlice.getNumber(), sortResponse);

	}

	// 게시글 상세 조회
	@Transactional
	public EventDetailResponseDto getEventDetail(Long eventId, Principal principal) {
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		String userId = SecurityUtil.getCurrentMemberUsernameOrAnonymous();

		EventDetailResponseDto responseDto = EventDetailResponseDto.toDto(clubEvent, false);

		if (!isAnonymous(userId)) {
			updateLikeStatusForUser(responseDto, userId);
		}

		return responseDto;
	}

	private boolean isAnonymous(String userId) {
		return "Anonymous".equals(userId);
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

		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 사용자가 구독하는 모든 토픽 가져오기
		List<TopicMember> subscribedTopicMembers = topicMemberRepository.findByMemberWithTopic(member);

		// 토픽 멤버에서 토픽만 추출하여 Type 열거형 리스트로 변환
		List<Type> subscribedTypes = subscribedTopicMembers.stream()
			.map(TopicMember::getTopic)
			.map(Topic::getType)
			.collect(Collectors.toList());

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
		Map<Long, Boolean> likedEventMap = eventLikeService.getLikedEventMap(member);

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

	// 유저의 찜한 이벤트 목록 조회
	@Transactional(readOnly = true)
	public SliceResponse<EventResponseDto> getLikedEvents(String type, String keyword, Pageable pageable, Principal principal) {
		// 사용자가 로그인하지 않은 경우
		if (principal == null) {
			throw new CustomException(CustomErrorCode.LOGIN_NEEDED);
		}

		String userEmail = principal.getName();
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));
		List<EventLike> likedEvents = eventLikeService.getLikedEvents(member);

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
		Map<Long, Boolean> likedEventMap = eventLikeService.getLikedEventMap(member);

		// 각 이벤트 DTO에 사용자의 찜 여부 설정
		for (EventResponseDto dto : eventResponseDtoList) {
			dto.setStar(likedEventMap.getOrDefault(dto.getEventId(), false));
		}
	}

	// EventDetailResponseDto(상세페이지)에 대한 찜한 상태 업데이트
	public void updateLikeStatusForUser(EventDetailResponseDto eventDetailResponseDto, String userEmail) {
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));
		Map<Long, Boolean> likedEventMap = eventLikeService.getLikedEventMap(member);

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
		Map<Long, Boolean> likedEventMap = eventLikeService.getLikedEventMap(member);

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
		Map<Long, Boolean> likedEventMap = eventLikeService.getLikedEventMap(member);

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
}
