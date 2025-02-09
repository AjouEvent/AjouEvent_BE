package com.example.ajouevent.service;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.dto.TabReadStatusResponse;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TopicMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class SubscriptionService {
	private final MemberRepository memberRepository;
	private final TopicMemberRepository topicMemberRepository;
	private final KeywordMemberRepository keywordMemberRepository;

	@Transactional(readOnly = true)
	public TabReadStatusResponse isSubscribedTabRead() {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// TopicMember와 KeywordMember에서 isRead가 false인 항목이 있는지 확인
		boolean hasUnreadTopics = topicMemberRepository.existsByMemberAndIsReadFalse(member);
		boolean hasUnreadKeywords = keywordMemberRepository.existsByMemberAndIsReadFalse(member);

		// 둘 중 하나라도 읽지 않은 상태라면 false 반환
		boolean isSubscribedTabRead = !(hasUnreadTopics || hasUnreadKeywords);

		return new TabReadStatusResponse(isSubscribedTabRead);
	}
}
