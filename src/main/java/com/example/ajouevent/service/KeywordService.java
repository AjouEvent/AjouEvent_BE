package com.example.ajouevent.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.domain.KeywordToken;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.dto.KeywordRequest;
import com.example.ajouevent.dto.KeywordResponse;
import com.example.ajouevent.dto.UnsubscribeKeywordRequest;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.logger.KeywordLogger;
import com.example.ajouevent.logger.TopicLogger;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.KeywordRepository;
import com.example.ajouevent.repository.KeywordTokenBulkRepository;
import com.example.ajouevent.repository.KeywordTokenRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TopicRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeywordService {
	private final TopicRepository topicRepository;
	private final MemberRepository memberRepository;

	private final KeywordLogger keywordLogger;

	private final KeywordRepository keywordRepository;
	private final KeywordMemberRepository keywordMemberRepository;
	private final KeywordTokenBulkRepository keywordTokenBulkRepository;
	private final KeywordTokenRepository keywordTokenRepository;
	private final TopicLogger topicLogger;

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

		topicLogger.log("가져온 topic: " + topic.getKoreanTopic());

		// 이미 해당 키워드를 구독 중인지 확인
		if (keywordMemberRepository.existsByKeywordAndMember(keyword, member)) {
			throw new CustomException(CustomErrorCode.ALREADY_SUBSCRIBED_KEYWORD);
		}

		// 사용자가 이미 구독한 키워드 개수를 확인
		long subscribedKeywordCount = keywordMemberRepository.countByMember(member);
		if (subscribedKeywordCount >= 10) {
			throw new CustomException(CustomErrorCode.MAX_KEYWORD_LIMIT_EXCEEDED);
		}

		List<Token> memberTokens = member.getTokens();
		KeywordMember keywordMember = KeywordMember.builder()
			.keyword(keyword)
			.member(member)
			.isRead(false)
			.lastReadAt(LocalDateTime.now())
			.build();
		keywordMemberRepository.save(keywordMember);

		// 토픽과 토큰을 매핑하여 저장 -> 사용자가 가지고 있는 토큰들이 topic을 구독
		List<KeywordToken> keywordTokens = memberTokens.stream()
			.map(token -> new KeywordToken(keyword, token))
			.collect(Collectors.toList());
		keywordTokenBulkRepository.saveAll(keywordTokens);

		keywordLogger.log("키워드 구독 : " + keyword.getKoreanKeyword());
	}

	// 새로운 키워드 생성 메서드
	private Keyword createNewTopic(KeywordRequest keywordRequest, String searchKeyword, String formattedKeyword, Topic topic) {
		// 새로운 토픽 생성 로직
		Keyword newKeyword = Keyword.builder()
			.encodedKeyword(formattedKeyword)
			.koreanKeyword(keywordRequest.getKoreanKeyword())
			.searchKeyword(searchKeyword)
			.topic(topic)
			.build();
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

		// 유저가 설정한 키워드를 찾아서 삭제
		keywordMemberRepository.deleteByKeywordAndMember(keyword, member);

		// 해당 키워드에 관련된 토큰을 찾아서 삭제
		List<Token> memberTokens = member.getTokens();
		keywordTokenRepository.deleteByKeywordAndTokens(keyword, memberTokens);
		keywordLogger.log("키워드 구독 취소 : " + keyword.getKoreanKeyword());
	}

	// 사용자가 설정한 키워드 조회
	@Transactional(readOnly = true)
	public List<KeywordResponse> getUserKeyword(Principal principal) {
		String memberEmail = principal.getName();
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		List<KeywordMember> keywordMembers = keywordMemberRepository.findByMemberWithKeywordAndTopic(member);

		return keywordMembers.stream()
			.map(km -> KeywordResponse.builder()
				.encodedKeyword(km.getKeyword().getEncodedKeyword())
				.koreanKeyword(km.getKeyword().getKoreanKeyword())
				.searchKeyword(km.getKeyword().getSearchKeyword())
				.topicName(km.getKeyword().getTopic().getKoreanTopic())
				.isRead(km.isRead())
				.lastReadAt(km.getLastReadAt())
				.build())
			.collect(Collectors.toList());
	}

	// 사용자의 Keyword 구독 목록 초기화
	@Transactional
	public void resetAllSubscriptions() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		Member member = memberRepository.findByEmailWithTokens(email)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		List<KeywordMember> keywordMembers = keywordMemberRepository.findByMemberWithKeyword(member);
		List<Token> tokens = member.getTokens();

		List<Long> tokenIds = tokens.stream()
			.map(Token::getId)
			.collect(Collectors.toList());
		keywordTokenRepository.deleteAllByTokenIds(tokenIds);

		List<Long> keywordMemberIds = keywordMembers.stream()
			.map(KeywordMember::getId)
			.collect(Collectors.toList());
		keywordMemberRepository.deleteAllByIds(keywordMemberIds);
	}
}
