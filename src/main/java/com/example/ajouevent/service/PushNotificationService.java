package com.example.ajouevent.service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.ajouevent.domain.ClubEvent;
import com.example.ajouevent.domain.JobStatus;
import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.domain.KeywordToken;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.NotificationType;
import com.example.ajouevent.domain.PushCluster;
import com.example.ajouevent.domain.PushClusterToken;
import com.example.ajouevent.domain.PushNotification;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.domain.TopicToken;
import com.example.ajouevent.dto.KeywordNotificationResponse;
import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.NotificationClickRequest;
import com.example.ajouevent.dto.SliceResponse;
import com.example.ajouevent.dto.TopicNotificationResponse;
import com.example.ajouevent.dto.UnreadNotificationCountResponse;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.EventRepository;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.KeywordRepository;
import com.example.ajouevent.repository.KeywordTokenRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.PushClusterRepository;
import com.example.ajouevent.repository.PushClusterTokenBulkRepository;
import com.example.ajouevent.repository.PushNotificationBulkRepository;
import com.example.ajouevent.repository.PushNotificationRepository;
import com.example.ajouevent.repository.TopicMemberRepository;
import com.example.ajouevent.repository.TopicRepository;
import com.example.ajouevent.repository.TopicTokenRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

	private static final String DEFAULT_IMAGE_URL = "https://www.ajou.ac.kr/_res/ajou/kr/img/intro/img-symbol.png";
	private static final String REDIRECTION_URL_PREFIX = "https://www.ajouevent.com/event/";
	private static final String DEFAULT_CLICK_ACTION_URL =  "https://www.ajouevent.com";

	private final TopicRepository topicRepository;
	private final TopicMemberRepository topicMemberRepository;
	private final PushClusterRepository pushClusterRepository;
	private final PushNotificationRepository pushNotificationRepository;
	private final PushClusterTokenBulkRepository pushClusterTokenBulkRepository;
	private final EventRepository eventRepository;
	private final TopicTokenRepository topicTokenRepository;
	private final KeywordTokenRepository keywordTokenRepository;
	private final KeywordRepository keywordRepository;
	private final KeywordMemberRepository keywordMemberRepository;
	private final MemberRepository memberRepository;
	private final FCMService fcmService;
	private final PushNotificationBulkRepository pushNotificationBulkRepository;

	@Transactional
	public Long postPushNotification(NoticeDto noticeDto, Long eventId) {

		// 푸시 알림 메시지 구성
		String title = composeMessageTitle(noticeDto);
		String body = composeBody(noticeDto);
		String imageUrl = getFirstImageUrl(noticeDto);
		String clickUrl = getRedirectionUrl(noticeDto, eventId);

		// Topic 찾기
		Topic topic = topicRepository.findByDepartment(noticeDto.getEnglishTopic())
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		// Topic을 구독 중이고, 알림을 수신 허용한 TopicMember 조회
		List<TopicMember> topicMembers = topicMemberRepository.findByTopicWithNotificationEnabledAndTokens(topic);

		// Topic을 구독 중인 TopicToken 조회 (isDeleted가 false)
		List<TopicToken> topicTokens = topicTokenRepository.findByTopicWithValidTokensAndReceiveNotificationTrue(topic);

		// ClubEvent 조회
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		// PushCluster 생성
		PushCluster pushCluster = PushCluster.builder()
			.clubEvent(clubEvent)
			.title(title)
			.body(body)
			.imageUrl(imageUrl)
			.clickUrl(clickUrl)
			.totalCount(topicTokens.size())
			.registeredAt(LocalDateTime.now())
			.jobStatus(JobStatus.PENDING)
			.build();
		pushClusterRepository.save(pushCluster);

		// 이 시점에서 pushCluster.getId()를 바로 사용할 수 있음
		Long pushClusterId = pushCluster.getId();
		log.info("토픽에서 - Generated PushCluster ID: {}", pushClusterId);

		// PushClusterTokens 생성
		List<PushClusterToken> clusterTokens = topicTokens.stream()
			.map(token -> PushClusterToken.builder()
				.pushCluster(pushCluster)
				.token(token.getToken()) // TopicToken에 연결된 Token 가져오기
				.jobStatus(JobStatus.PENDING) // 초기 상태: PENDING
				.requestTime(LocalDateTime.now()) // 요청 시간 기록
				.build())
			.collect(Collectors.toList());
		pushClusterTokenBulkRepository.saveAll(clusterTokens);

		// PushNotifications 생성
		List<PushNotification> notifications = topicMembers.stream()
			.map(member -> PushNotification.builder()
				.pushCluster(pushCluster)
				.member(member.getMember())
				.topic(topic)
				.title(title)
				.body(body)
				.imageUrl(imageUrl)
				.clickUrl(clickUrl)
				.notificationType(NotificationType.TOPIC)
				.notifiedAt(LocalDateTime.now())
				.build())
			.collect(Collectors.toList());
		pushNotificationBulkRepository.saveAll(notifications);

		return pushCluster.getId(); // PushCluster ID 반환
	}

	private String composeMessageTitle(NoticeDto noticeDto) {
		return String.format("[%s]", noticeDto.getKoreanTopic());
	}

	private String composeBody(NoticeDto noticeDto) {
		return noticeDto.getTitle();
	}

	private String getFirstImageUrl(NoticeDto noticeDto) {
		List<String> images = Optional.ofNullable(noticeDto.getImages())
			.filter(imgs -> !imgs.isEmpty())
			.orElseGet(() -> {
				List<String> defaultImages = new ArrayList<>();
				defaultImages.add(DEFAULT_IMAGE_URL);
				return defaultImages;
			});
		return images.get(0);
	}

	private String getRedirectionUrl(NoticeDto noticeDto, Long eventId) {
		String url = Optional.ofNullable(noticeDto.getUrl())
			.filter(u -> !u.isEmpty())
			.map(u -> REDIRECTION_URL_PREFIX + eventId) // 크롤링 후 DB에 저장된, 우리 앱 상세페이지로 이동
			.orElse(DEFAULT_CLICK_ACTION_URL);
		log.info("리다이렉션하는 URL: {}", url);
		return url;
	}

	@Transactional
	public void handleKeywordPushNotification(NoticeDto noticeDto, Long eventId) {

		// Topic 찾기
		Topic topic = topicRepository.findByDepartment(noticeDto.getEnglishTopic())
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		// 해당 Topic에 설정된 모든 Keyword 가져오기
		List<Keyword> keywords = keywordRepository.findByTopic(topic);

		// 크롤링된 제목에서 키워드 매칭
		List<Keyword> matchedKeywords = keywords.stream()
			.filter(keyword -> noticeDto.getTitle().contains(keyword.getKoreanKeyword()))
			.collect(Collectors.toList());

		if (matchedKeywords.isEmpty()) {
			log.info("매칭된 키워드가 없습니다. 푸시 알림을 발송하지 않습니다.");
			return;
		}

		// ClubEvent 조회
		ClubEvent clubEvent = eventRepository.findById(eventId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.EVENT_NOT_FOUND));

		for (Keyword keyword : matchedKeywords) {
			String title = keyword.getKoreanKeyword() + "-" + composeMessageTitle(noticeDto);
			String body = composeBody(noticeDto);
			String imageUrl = getFirstImageUrl(noticeDto);
			String clickUrl = getRedirectionUrl(noticeDto, eventId);

			// 해당 키워드에 구독된 사용자 조회
			List<KeywordMember> keywordMembers = keywordMemberRepository.findByKeyword(keyword);
			List<KeywordToken> keywordTokens = keywordTokenRepository.findKeywordTokensWithTokenByKeyword(keyword);

			// PushCluster 생성
			PushCluster pushCluster = PushCluster.builder()
				.clubEvent(clubEvent)
				.title(title)
				.body(body)
				.imageUrl(imageUrl)
				.clickUrl(clickUrl)
				.totalCount(keywordTokens.size())
				.registeredAt(LocalDateTime.now())
				.jobStatus(JobStatus.PENDING)
				.build();
			pushClusterRepository.save(pushCluster);

			// 이 시점에서 pushCluster.getId()를 바로 사용할 수 있음
			Long pushClusterId = pushCluster.getId();
			log.info("키워드에서 - Generated PushCluster ID: {}", pushClusterId);

			// PushClusterToken 생성
			List<PushClusterToken> clusterTokens = keywordTokens.stream()
				.map(token -> PushClusterToken.builder()
					.pushCluster(pushCluster)
					.token(token.getToken())
					.jobStatus(JobStatus.PENDING)
					.requestTime(LocalDateTime.now())
					.build())
				.collect(Collectors.toList());

			// PushNotification 생성
			List<PushNotification> notifications = keywordMembers.stream()
				.map(member -> PushNotification.builder()
					.pushCluster(pushCluster)
					.member(member.getMember())
					.keyword(keyword)
					.title(title)
					.body(body)
					.imageUrl(imageUrl)
					.topic(topic)
					.clickUrl(clickUrl)
					.notificationType(NotificationType.KEYWORD)
					.notifiedAt(LocalDateTime.now())
					.build())
				.collect(Collectors.toList());
			pushNotificationBulkRepository.saveAll(notifications);

			// FCM 푸시 알림 전송
			fcmService.sendKeywordPushNotification(clusterTokens, pushCluster, keyword, noticeDto, eventId);

		}
	}

	// Topic 알림 조회
	@Transactional
	public SliceResponse<TopicNotificationResponse> getTopicNotificationsForMember(Pageable pageable) {
		Member member = getAuthenticatedMember();
		return getTopicNotifications(member, pageable);
	}

	// Keyword 알림 조회
	@Transactional
	public SliceResponse<KeywordNotificationResponse> getKeywordNotificationsForMember(Pageable pageable) {
		Member member = getAuthenticatedMember();
		return getKeywordNotifications(member, pageable);
	}

	private SliceResponse<TopicNotificationResponse> getTopicNotifications(Member member, Pageable pageable) {
		Slice<PushNotification> notificationSlice = pushNotificationRepository.findByMemberAndNotificationType(member, NotificationType.TOPIC, pageable);

		// 기존에 조회한 데이터에서 읽지 않은 알림만 필터링
		List<PushNotification> unreadNotifications = notificationSlice.getContent()
			.stream()
			.filter(notification -> !notification.isRead()) // 읽지 않은 알림만 선택
			.toList();

		if (!unreadNotifications.isEmpty()) {
			pushNotificationBulkRepository.updateReadStatus(unreadNotifications);
		}

		List<TopicNotificationResponse> responseList = notificationSlice.getContent().stream()
			.map(TopicNotificationResponse::toDto)
			.collect(Collectors.toList());

		return new SliceResponse<>(
			responseList,
			notificationSlice.hasPrevious(),
			notificationSlice.hasNext(),
			notificationSlice.getNumber(),
			createSortResponse(pageable)
		);
	}

	private SliceResponse<KeywordNotificationResponse> getKeywordNotifications(Member member, Pageable pageable) {
		Slice<PushNotification> notificationSlice = pushNotificationRepository.findByMemberAndNotificationType(member, NotificationType.KEYWORD, pageable);

		// 기존에 조회한 데이터에서 읽지 않은 알림만 필터링
		List<PushNotification> unreadNotifications = notificationSlice.getContent()
			.stream()
			.filter(notification -> !notification.isRead()) // 읽지 않은 알림만 선택
			.toList();

		if (!unreadNotifications.isEmpty()) {
			pushNotificationBulkRepository.updateReadStatus(unreadNotifications);
		}

		List<KeywordNotificationResponse> responseList = notificationSlice.getContent().stream()
			.map(KeywordNotificationResponse::toDto)
			.collect(Collectors.toList());

		return new SliceResponse<>(
			responseList,
			notificationSlice.hasPrevious(),
			notificationSlice.hasNext(),
			notificationSlice.getNumber(),
			createSortResponse(pageable)
		);
	}

	private SliceResponse.SortResponse createSortResponse(Pageable pageable) {
		return SliceResponse.SortResponse.builder()
			.sorted(pageable.getSort().isSorted())
			.direction(String.valueOf(pageable.getSort().descending()))
			.orderProperty(pageable.getSort().stream().map(Sort.Order::getProperty).findFirst().orElse(null))
			.build();
	}

	private Member getAuthenticatedMember() {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		return memberRepository.findByEmailWithTokens(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));
	}

	@Transactional
	public void markNotificationAsRead(NotificationClickRequest notificationClickRequest) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();

		Member member = memberRepository.findByEmailWithTokens(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		PushNotification notification = pushNotificationRepository.findByMemberAndId(member, notificationClickRequest.getPushNotificationId())
			.orElseThrow(() -> new CustomException(CustomErrorCode.PUSH_NOTIFICATION_NOT_FOUND));

		notification.markAsRead();
		pushNotificationRepository.save(notification);
	}

	@Transactional
	public UnreadNotificationCountResponse getUnreadNotificationCount(Principal principal) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();

		Member member = memberRepository.findByEmailWithTokens(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		int unreadNotificationCount = pushNotificationRepository.countByMemberAndIsReadFalse(member);

		return UnreadNotificationCountResponse.builder()
			.unreadNotificationCount(unreadNotificationCount)
			.build();
	}

	@Transactional
	public void markAllNotificationsAsRead() {
		Member member = getAuthenticatedMember();

		List<PushNotification> notifications = pushNotificationRepository.findByMemberAndIsReadFalse(member);

		if (notifications.isEmpty()) {
			return;
		}

		pushNotificationBulkRepository.updateReadStatus(notifications);
	}

}
