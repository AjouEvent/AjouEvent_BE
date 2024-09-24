package com.example.ajouevent.service;

import java.security.Principal;
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
	private final FCMService fcmService;

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
		String englishKeyword = keywordRequest.getEnglishKeyword();
		String topicName = keywordRequest.getTopicName();

		Member member = memberRepository.findByEmailWithTokens(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 공백을 "8"로 바꿉니다
		String formattedKeyword = englishKeyword.replace(" ", "8") + "8" + topicName;

		Topic topic = topicRepository.findByDepartment(topicName)
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		// 입력된 키워드가 존재하는지 확인하고, 없다면 새로 생성
		Keyword keyword = keywordRepository.findByEnglishKeyword(formattedKeyword)
			.orElseGet(() -> createNewTopic(keywordRequest, formattedKeyword, topic));

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
			.build();
		keywordMemberRepository.save(keywordMember);

		// 토픽과 토큰을 매핑하여 저장 -> 사용자가 가지고 있는 토큰들이 topic을 구독
		List<KeywordToken> keywordTokens = memberTokens.stream()
			.map(token -> new KeywordToken(keyword, token))
			.collect(Collectors.toList());
		keywordTokenBulkRepository.saveAll(keywordTokens);

		// FCM 서비스를 사용하여 토픽에 대한 구독 진행
		List<String> tokenValues = memberTokens.stream()
			.map(Token::getTokenValue)
			.collect(Collectors.toList());
		fcmService.subscribeToTopic(formattedKeyword, tokenValues);

		keywordLogger.log("키워드 구독 : " + keyword.getKoreanKeyword());
	}

	// 새로운 키워드 생성 메서드
	private Keyword createNewTopic(KeywordRequest keywordRequest, String formattedKeyword, Topic topic) {
		// 새로운 토픽 생성 로직
		Keyword newKeyword = Keyword.builder()
			.englishKeyword(formattedKeyword)
			.koreanKeyword(keywordRequest.getKoreanKeyword())
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
		String englishKeyword = unsubscribeKeywordRequest.getEnglishKeyword();

		Member member = memberRepository.findByEmailWithTokens(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Keyword keyword = keywordRepository.findByEnglishKeyword(englishKeyword)
			.orElseThrow(() -> new CustomException(CustomErrorCode.KEYWORD_NOT_FOUND));

		// 유저가 설정한 키워드를 찾아서 삭제
		keywordMemberRepository.deleteByKeywordAndMember(keyword, member);

		// 해당 키워드에 관련된 토큰을 찾아서 삭제
		List<Token> memberTokens = member.getTokens();
		keywordTokenRepository.deleteByKeywordAndTokens(keyword, memberTokens);

		// FCM 서비스를 사용하여 키워드에 대한 구독 취소 진행
		List<String> tokenValues = memberTokens.stream()
			.map(Token::getTokenValue)
			.collect(Collectors.toList());
		fcmService.unsubscribeFromTopic(englishKeyword, tokenValues);
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
				.koreanKeyword(km.getKeyword().getKoreanKeyword())
				.englishKeyword(km.getKeyword().getEnglishKeyword())
				.topicName(km.getKeyword().getTopic().getKoreanTopic())
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
		List<String> tokenValues = tokens.stream()
			.map(Token::getTokenValue)
			.toList();

		keywordMembers.forEach(keywordMember -> {
			fcmService.unsubscribeFromTopic(keywordMember.getKeyword().getEnglishKeyword(), tokenValues);
			keywordLogger.log("Keyword 구독 초기화 - Member: " + keywordMember.getMember().getEmail() + ", Keyword: "
				+ keywordMember.getKeyword().getKoreanKeyword() + " - " + keywordMember.getKeyword().getEnglishKeyword());
		});

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
