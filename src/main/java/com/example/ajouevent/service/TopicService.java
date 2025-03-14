package com.example.ajouevent.service;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordMember;
import com.example.ajouevent.domain.KeywordToken;
import com.example.ajouevent.dto.NotificationPreferenceRequest;
import com.example.ajouevent.dto.TopicDetailResponse;
import com.example.ajouevent.dto.TopicStatus;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicMember;
import com.example.ajouevent.domain.TopicToken;
import com.example.ajouevent.dto.MemberDto;
import com.example.ajouevent.dto.TopicRequest;
import com.example.ajouevent.dto.TopicResponse;
import com.example.ajouevent.logger.TopicLogger;
import com.example.ajouevent.repository.KeywordMemberRepository;
import com.example.ajouevent.repository.KeywordTokenBulkRepository;
import com.example.ajouevent.repository.KeywordTokenRepository;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TokenRepository;
import com.example.ajouevent.repository.TopicMemberRepository;
import com.example.ajouevent.repository.TopicRepository;
import com.example.ajouevent.repository.TopicTokenBulkRepository;
import com.example.ajouevent.repository.TopicTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicService {
	private final TokenRepository tokenRepository;
	private final MemberRepository memberRepository;
	private final TopicRepository topicRepository;
	private final TopicTokenRepository topicTokenRepository;
	private final TopicMemberRepository topicMemberRepository;
	private final TopicTokenBulkRepository topicTokenBulkRepository;
	private final KeywordTokenRepository keywordTokenRepository;
	private final KeywordMemberRepository keywordMemberRepository;
	private final KeywordTokenBulkRepository keywordTokenBulkRepository;
	private final TopicLogger topicLogger;

	// 토큰 만료 기간 상수 정의
	private static final int TOKEN_EXPIRATION_WEEKS = 10;

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

		List<Token> memberTokens = member.getTokens();
		TopicMember topicMember = TopicMember.builder()
			.topic(topic)
			.member(member)
			.isRead(false)
			.lastReadAt(LocalDateTime.now())
			.receiveNotification(true)
			.build();
		topicMemberRepository.save(topicMember);

		// 토픽과 토큰을 매핑하여 저장 -> 사용자가 가지고 있는 토큰들이 topic을 구독
		List<TopicToken> topicTokens = memberTokens.stream()
			.map(token -> new TopicToken(topic, token))
			.collect(Collectors.toList());
		topicTokenBulkRepository.saveAll(topicTokens);
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

		List<Token> memberTokens = member.getTokens();

		// 사용자가 구독하고 있는 토픽에 대한 TopicToken만 삭제
		topicTokenRepository.deleteByTopicAndTokens(topic, memberTokens);

		// 멤버가 구독하고 있는 해당 토픽을 찾아서 삭제
		topicMemberRepository.deleteByTopicAndMember(topic, member);
	}

	@Transactional
	public void saveFCMToken(MemberDto.LoginRequest loginRequest) {
		log.info("saveFCMToken 메서드 호출");

		// 사용자 조회
		Member member = memberRepository.findByEmail(loginRequest.getEmail())
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// token이 이미 있는지 체크
		Optional<Token> existingToken = tokenRepository.findByTokenValueAndMember(loginRequest.getFcmToken(), member);
		if (existingToken.isPresent()) {
			Token token = existingToken.get();
			log.info("이미 존재하는 토큰: " + existingToken.get().getTokenValue());
			token.setExpirationDate(LocalDate.now().plusWeeks(TOKEN_EXPIRATION_WEEKS));
			tokenRepository.save(token);
		} else {
			// Only create and save a new token if it does not exist
			Token token = Token.builder()
				.tokenValue(loginRequest.getFcmToken())
				.member(member)
				.expirationDate(LocalDate.now().plusWeeks(TOKEN_EXPIRATION_WEEKS))
				.isDeleted(false)
				.build();
			log.info("DB에 저장하는 token : " + token.getTokenValue());
			tokenRepository.save(token);

			// 사용자가 구독 중인 모든 토픽을 가져옴
			List<TopicMember> topicMembers = topicMemberRepository.findByMemberWithTopic(member);
			List<Topic> subscribedTopics = topicMembers.stream()
				.map(TopicMember::getTopic)
				.distinct()
				.toList();

			// 사용자가 구독 중인 모든 키워드를 가져옴
			List<KeywordMember> keywordMembers = keywordMemberRepository.findByMemberWithKeyword(member);
			List<Keyword> subscribedKeywords = keywordMembers.stream()
				.map(KeywordMember::getKeyword)
				.distinct()
				.toList();

			// 새 토큰을 기존에 구독된 모든 토픽과 매핑하여 TopicToken 생성 및 저장
			List<TopicToken> newTopicSubscriptions = subscribedTopics.stream()
				.map(topic -> new TopicToken(topic, token))
				.collect(Collectors.toList());
			topicTokenBulkRepository.saveAll(newTopicSubscriptions);

			// 새 토큰을 기존에 구독된 모든 키워드와 매핑하여 KeywordToken 생성 및 저장
			List<KeywordToken> newKeywordSubscriptions = subscribedKeywords.stream()
				.map(keyword -> new KeywordToken(keyword, token))
				.collect(Collectors.toList());
			keywordTokenBulkRepository.saveAll(newKeywordSubscriptions);
		}
	}

	// 매일 오전 5시에 실행 (cron 표현식 사용)
	@Scheduled(cron = "0 0 5 * * ?")
	@Transactional
	public void unsubscribeExpiredTokens() {
		LocalDate now = LocalDate.now();
		log.info("오늘의 날짜 : " + now);

		// 만료된 토큰을 가져옵니다.
		List<Token> expiredTokens = tokenRepository.findByExpirationDate(now);

		// 만료된 토큰과 관련된 모든 TopicToken을 찾음
		List<TopicToken> topicTokens = topicTokenRepository.findTopicTokensWithTopic(expiredTokens);

		// 만료된 토큰과 관련된 모든 KeywordToken을 찾음
		List<KeywordToken> keywordTokens = keywordTokenRepository.findKeywordTokensWithKeyword(expiredTokens);

		// 만료된 토큰의 값들을 추출
		List<String> tokenValues = expiredTokens.stream()
			.map(Token::getTokenValue)
			.collect(Collectors.toList());

		// 만료된 토큰 ID 리스트 추출
		List<Long> expiredTokenIds = expiredTokens.stream()
			.map(Token::getId)
			.collect(Collectors.toList());

		topicTokenRepository.deleteAllByTokenIds(expiredTokenIds); // TopicTokenRepository에서 먼저 삭제하고 TokenRepository에서 삭제
		keywordTokenRepository.deleteAllByTokenIds(expiredTokenIds); // KeywordTokenRepository에서 먼저 삭제하고 TokenRepository에서 삭제
		// 만료된 토큰 삭제
		tokenRepository.deleteAllByTokenIds(expiredTokenIds);
	}

	// 사용자의 Topic 구독 목록 초기화
	@Transactional
	public void resetAllSubscriptions() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		Member member = memberRepository.findByEmailWithTokens(email)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		List<TopicMember> topicMembers = topicMemberRepository.findByMemberWithTopic(member);
		List<Token> tokens = member.getTokens();

		List<Long> tokenIds = tokens.stream()
			.map(Token::getId)
			.collect(Collectors.toList());
		topicTokenRepository.deleteAllByTokenIds(tokenIds);

		List<Long> topicMemberIds = topicMembers.stream()
			.map(TopicMember::getId)
			.collect(Collectors.toList());
		topicMemberRepository.deleteAllByIds(topicMemberIds);
	}

	// 사용자가 구독하고 있는 토픽 조회
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

	@Transactional
	public void updateNotificationPreference(NotificationPreferenceRequest request) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Topic topic = topicRepository.findByDepartment(request.getTopic())
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));

		TopicMember topicMember = topicMemberRepository.findByMemberAndTopic(member, topic)
			.orElseThrow(() -> new CustomException(CustomErrorCode.SUBSCRIBE_FAILED));

		topicMember.setReceiveNotification(request.isReceiveNotification());
	}
}
