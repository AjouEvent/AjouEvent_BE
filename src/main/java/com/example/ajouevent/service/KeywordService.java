package com.example.ajouevent.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.dto.KeywordRequest;
import com.example.ajouevent.dto.UnsubscribeKeywordRequest;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.logger.KeywordLogger;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.KeywordRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TopicRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeywordService {
	private final TokenSubscriptionService tokenSubscriptionService;
	private final TopicRepository topicRepository;
	private final MemberRepository memberRepository;
	private final KeywordRepository keywordRepository;
	private final KeywordMemberRepository keywordMemberRepository;
	private final KeywordLogger keywordLogger;

	// 키워드 구독 - 키워드 하나씩
	@Transactional
	public void subscribeToKeyword(KeywordRequest keywordRequest) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		String koreanKeyword = keywordRequest.getKoreanKeyword();
		String topicName = keywordRequest.getTopicName();

		Member member = memberRepository.findByEmailWithValidTokens(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// URL 인코딩과 Topic ID 결합하여 고유한 formattedKeyword 생성
		String encodedKeyword = URLEncoder.encode(koreanKeyword, StandardCharsets.UTF_8);
		String searchKeyword = koreanKeyword + "_" + topicName;
		encodedKeyword = encodedKeyword.replace("+", "%20");
		String formattedKeyword = encodedKeyword + "_" + topicName;

		Topic topic = topicRepository.findByDepartment(topicName)
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		// 입력된 키워드가 존재하는지 확인하고, 없다면 새로 생성
		Keyword keyword = keywordRepository.findByEncodedKeyword(formattedKeyword)
			.orElseGet(() -> createNewTopic(keywordRequest, searchKeyword, formattedKeyword, topic));

		keywordLogger.log("가져온 topic: " + topic.getKoreanTopic());

		// 이미 해당 키워드를 구독 중인지 확인
		if (keywordMemberRepository.existsByKeywordAndMember(keyword, member)) {
			throw new CustomException(CustomErrorCode.ALREADY_SUBSCRIBED_KEYWORD);
		}

		// 사용자가 이미 구독한 키워드 개수를 확인
		long subscribedKeywordCount = keywordMemberRepository.countByMember(member);
		if (subscribedKeywordCount >= 10) {
			throw new CustomException(CustomErrorCode.MAX_KEYWORD_LIMIT_EXCEEDED);
		}

		KeywordMember keywordMember = KeywordMember.create(keyword, member);
		keywordMemberRepository.save(keywordMember);

		tokenSubscriptionService.subscribeTokenToKeyword(member, keyword);

		keywordLogger.log("키워드 구독 : " + keyword.getKoreanKeyword());
	}

	// 새로운 키워드 생성 메서드
	private Keyword createNewTopic(KeywordRequest keywordRequest, String searchKeyword, String formattedKeyword, Topic topic) {
		// 새로운 토픽 생성 로직
		Keyword newKeyword = Keyword.create(
			formattedKeyword,
			keywordRequest.getKoreanKeyword(),
			searchKeyword,
			topic
		);
		keywordRepository.save(newKeyword);

		keywordLogger.log("새로운 키워드 생성 : " + newKeyword.getKoreanKeyword());
		return newKeyword;
	}

	// 키워드 구독 취소 - 키워드 하나씩
	@Transactional
	public void unsubscribeFromKeyword(UnsubscribeKeywordRequest unsubscribeKeywordRequest) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		String encodedKeyword = unsubscribeKeywordRequest.getEncodedKeyword();

		Member member = memberRepository.findByEmailWithTokens(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Keyword keyword = keywordRepository.findByEncodedKeyword(encodedKeyword)
			.orElseThrow(() -> new CustomException(CustomErrorCode.KEYWORD_NOT_FOUND));

		keywordMemberRepository.deleteByKeywordAndMember(keyword, member);

		tokenSubscriptionService.unsubscribeTokenFromKeyword(member, keyword);
		keywordLogger.log("키워드 구독 취소 : " + keyword.getKoreanKeyword());
	}

	// 사용자의 Keyword 구독 목록 초기화
	@Transactional
	public void resetAllSubscriptions() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		Member member = memberRepository.findByEmailWithTokens(email)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		List<KeywordMember> keywordMembers = keywordMemberRepository.findByMemberWithKeyword(member);
		if (keywordMembers.isEmpty()) {
			log.info("사용자가 구독한 키워드 없음");
			return;
		}

		tokenSubscriptionService.unsubscribeTokensFromAllKeywords(member);

		List<Long> keywordMemberIds = keywordMembers.stream()
			.map(KeywordMember::getId)
			.collect(Collectors.toList());
		keywordMemberRepository.deleteAllByIds(keywordMemberIds);
	}

	@Transactional
	public void markKeywordAsRead(String searchKeyword, String userEmail) {
		Member member = memberRepository.findByEmail(userEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Keyword keyword = keywordRepository.findBySearchKeyword(searchKeyword)
			.orElseThrow(() -> new CustomException(CustomErrorCode.KEYWORD_NOT_FOUND));

		KeywordMember keywordMember = keywordMemberRepository.findByKeywordAndMember(keyword, member)
			.orElseThrow(() -> new CustomException(CustomErrorCode.KEYWORD_NOT_FOUND));
		if (keywordMember.isRead() == false) {
			keywordMember.markAsRead();
			keywordMemberRepository.save(keywordMember);
		}
	}
}
