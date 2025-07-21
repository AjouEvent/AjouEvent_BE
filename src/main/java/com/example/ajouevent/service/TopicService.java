package com.example.ajouevent.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.ajouevent.dto.NotificationPreferenceRequest;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.dto.TopicRequest;
import com.example.ajouevent.logger.TopicLogger;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TopicMemberRepository;
import com.example.ajouevent.repository.TopicRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicService {
	private final TokenSubscriptionService tokenSubscriptionService;
	private final MemberRepository memberRepository;
	private final TopicRepository topicRepository;
	private final TopicMemberRepository topicMemberRepository;
	private final TopicLogger topicLogger;

	// 토픽 구독 - 토픽 하나씩
	@Transactional
	public void subscribeToTopics(TopicRequest topicRequest) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		String topicName = topicRequest.getTopic();

		Member member = memberRepository.findByEmailWithValidTokens(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));
		Topic topic = topicRepository.findByDepartment(topicName)
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		// 이미 해당 토픽을 구독 중인지 확인
		if (topicMemberRepository.existsByTopicAndMember(topic, member)) {
			throw new CustomException(CustomErrorCode.ALREADY_SUBSCRIBED_TOPIC);
		}

		topicLogger.log(topic.getDepartment() + "토픽 구독");
		topicLogger.log("멤버 이메일 : " + memberEmail);

		TopicMember topicMember = TopicMember.create(member, topic);
		topicMemberRepository.save(topicMember);

		//  SubscriptionService를 호출하여 Token과 Topic 매핑
		tokenSubscriptionService.subscribeTokenToTopic(member, topic);
	}

	// 토픽 구독 취소 - 토픽 하나씩
	@Transactional
	public void unsubscribeFromTopics(TopicRequest topicRequest) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		String topicName = topicRequest.getTopic();

		Member member = memberRepository.findByEmailWithTokens(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Topic topic = topicRepository.findByDepartment(topicName)
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		topicLogger.log(topic.getDepartment() + "토픽 구독 취소");
		topicLogger.log("멤버 이메일 : " + memberEmail);

		// 멤버가 구독하고 있는 해당 토픽을 찾아서 삭제
		topicMemberRepository.deleteByTopicAndMember(topic, member);

		//  SubscriptionService를 호출하여 Token과 Topic 매핑 해제
		tokenSubscriptionService.unsubscribeTokenFromTopic(member, topic);
	}

	// 사용자의 Topic 구독 목록 초기화
	@Transactional
	public void resetAllSubscriptions() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		Member member = memberRepository.findByEmailWithTokens(email)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		List<TopicMember> topicMembers = topicMemberRepository.findByMemberWithTopic(member);
		if (topicMembers.isEmpty()) {
			log.info("사용자가 구독한 토픽 없음");
			return;
		}

		tokenSubscriptionService.unsubscribeTokensFromAllTopics(member);

		List<Long> topicMemberIds = topicMembers.stream()
			.map(TopicMember::getId)
			.collect(Collectors.toList());
		topicMemberRepository.deleteAllByIds(topicMemberIds);
	}

	@Transactional
	public void updateNotificationPreference(NotificationPreferenceRequest request) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Topic topic = topicRepository.findByDepartment(request.getTopic())
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		TopicMember topicMember = topicMemberRepository.findByMemberAndTopic(member, topic)
			.orElseThrow(() -> new CustomException(CustomErrorCode.SUBSCRIBE_FAILED));

		topicMember.changeReceiveNotification(request.isReceiveNotification());
	}

	@Transactional
	public void markTopicAsRead(String type, String userEmail) {
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Topic topic = topicRepository.findByDepartment(type)
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		Optional<TopicMember> optionalTopicMember = topicMemberRepository.findByMemberAndTopic(member, topic);
		if (optionalTopicMember.isPresent()) { // 사용자가 구독하고 있는 경우만 읽음 상태 갱신
			TopicMember topicMember = optionalTopicMember.get();
			topicMember.markAsRead();
			topicMemberRepository.save(topicMember);
		}
	}
}
