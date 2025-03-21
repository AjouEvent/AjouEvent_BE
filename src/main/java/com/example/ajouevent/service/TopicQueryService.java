package com.example.ajouevent.service;

import java.security.Principal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.dto.TopicDetailResponse;
import com.example.ajouevent.dto.TopicResponse;
import com.example.ajouevent.dto.TopicStatus;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TopicMemberRepository;
import com.example.ajouevent.repository.TopicRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicQueryService {
	private final MemberRepository memberRepository;
	private final TopicRepository topicRepository;
	private final TopicMemberRepository topicMemberRepository;

	// 사용자가 구독하고 있는 Topic 조회 - 컨트롤러 응답용
	@Transactional(readOnly = true)
	public List<TopicResponse> getSubscribedTopics() {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 회원이 구독하는 토픽 목록 조회
		List<TopicMember> topicMembers = topicMemberRepository.findByMemberWithTopic(member);

		List<TopicResponse> topicResponseList = topicMembers.stream()
			.map(topicMember -> new TopicResponse(
				topicMember.getId(),
				topicMember.getTopic().getKoreanTopic(),
				topicMember.getTopic().getDepartment(),
				topicMember.isRead(),
				topicMember.getLastReadAt()
			))
			.sorted(Comparator.comparing(TopicResponse::getId).reversed())
			.collect(Collectors.toList());
		return topicResponseList;
	}

	// 사용자가 구독한 Topic 조회 -
	@Transactional(readOnly = true)
	public List<Topic> getSubscribedTopics(Member member) {
		return topicMemberRepository.findByMemberWithTopic(member).stream()
			.map(TopicMember::getTopic)
			.distinct()
			.toList();
	}

	// 전체 Topic에 대해 사용자의 구독 여부 조회
	@Transactional(readOnly = true)
	public List<TopicStatus> getTopicWithUserSubscriptionsStatus(Principal principal) {
		List<Topic> allTopics = topicRepository.findAll();
		Member member = memberRepository.findByEmail(principal.getName())
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		List<TopicMember> subscriptions = topicMemberRepository.findByMember(member);
		Map<Long, Boolean> subscriptionMap = subscriptions.stream()
			.collect(Collectors.toMap(
				subscription -> subscription.getTopic().getId(),
				TopicMember::isReceiveNotification
			));

		List<TopicStatus> topicStatusList = allTopics.stream()
			.map(topic -> TopicStatus.builder()
				.id(topic.getId())
				.koreanTopic(topic.getKoreanTopic())
				.englishTopic(topic.getDepartment())
				.classification(topic.getClassification())
				.subscribed(subscriptionMap.containsKey(topic.getId()))
				.receiveNotification(subscriptionMap.getOrDefault(topic.getId(), false))  // 구독 안 했으면 기본값 false
				.koreanOrder(topic.getKoreanOrder())
				.build())
			.sorted(Comparator.comparingLong(TopicStatus::getKoreanOrder))
			.toList();
		return topicStatusList;
	}

	// 전체 topic 조회
	@Transactional(readOnly = true)
	public List<TopicDetailResponse> getAllTopics() {
		List<Topic> topics = topicRepository.findAll();

		List<TopicDetailResponse> topicDetailResponseList = topics.stream()
			.map(topic -> new TopicDetailResponse(
				topic.getClassification(),
				topic.getKoreanOrder(),
				topic.getKoreanTopic()
			))
			.sorted(Comparator.comparingLong(TopicDetailResponse::getKoreanOrder))
			.toList();
		return topicDetailResponseList;
	}
}