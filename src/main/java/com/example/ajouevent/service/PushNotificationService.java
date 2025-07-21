package com.example.ajouevent.service;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.ajouevent.dto.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.NotificationType;
import com.example.ajouevent.domain.PushCluster;
import com.example.ajouevent.domain.PushNotification;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.PushNotificationBulkRepository;
import com.example.ajouevent.repository.PushNotificationRepository;
import com.example.ajouevent.repository.TopicMemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushNotificationService {

	private final TopicMemberRepository topicMemberRepository;
	private final PushNotificationRepository pushNotificationRepository;
	private final KeywordMemberRepository keywordMemberRepository;
	private final MemberRepository memberRepository;
	private final PushNotificationBulkRepository pushNotificationBulkRepository;

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

	@Transactional
	public void saveTopicNotifications(PushCluster cluster) {
		// Topic을 구독 중이고, 알림을 수신 허용한 TopicMember 조회
		List<TopicMember> topicMembers = topicMemberRepository.findByTopicWithNotificationEnabledAndTokens(cluster.getTopic());

		List<PushNotification> notifications = topicMembers.stream()
				.map(member -> PushNotification.builder()
						.pushCluster(cluster)
						.member(member.getMember())
						.topic(cluster.getTopic())
						.title(cluster.getTitle())
						.body(cluster.getBody())
						.imageUrl(cluster.getImageUrl())
						.clickUrl(cluster.getClickUrl())
						.notificationType(NotificationType.TOPIC)
						.notifiedAt(LocalDateTime.now())
						.build())
				.toList();
		pushNotificationBulkRepository.saveAll(notifications);
	}

	@Transactional
	public void saveKeywordNotifications(List<PushCluster> clusters) {
		for (PushCluster cluster : clusters) {
			List<KeywordMember> keywordMembers = keywordMemberRepository.findByKeyword(cluster.getKeyword());
			List<PushNotification> notifications = keywordMembers.stream()
					.map(member -> PushNotification.builder()
							.pushCluster(cluster)
							.member(member.getMember())
							.keyword(cluster.getKeyword())
							.topic(cluster.getTopic())
							.title(cluster.getTitle())
							.body(cluster.getBody())
							.imageUrl(cluster.getImageUrl())
							.clickUrl(cluster.getClickUrl())
							.notificationType(NotificationType.KEYWORD)
							.notifiedAt(LocalDateTime.now())
							.build())
					.toList();
			pushNotificationBulkRepository.saveAll(notifications);
		}
	}

	@Transactional
	public Map<Long, Long> getUnreadNotificationCountMapForTopic(String koreanTopic) {
		return pushNotificationRepository.countUnreadNotificationsForTopic(koreanTopic)
				.stream()
				.collect(Collectors.toMap(
                        UnreadNotificationCountDto::getMemberId,
                        UnreadNotificationCountDto::getUnreadNotificationCount
				));
	}

	@Transactional
	public Map<Long, Long> getUnreadNotificationCountMapForKeyword(String encodedKeyword) {
		return pushNotificationRepository.countUnreadNotificationsForKeyword(encodedKeyword)
				.stream()
				.collect(Collectors.toMap(
                        UnreadNotificationCountDto::getMemberId,
                        UnreadNotificationCountDto::getUnreadNotificationCount
				));
	}

}
