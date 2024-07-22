package com.example.ajouevent.service;

import java.security.Principal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TokenRepository;
import com.example.ajouevent.repository.TopicMemberBulkRepository;
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
	private final TopicRepository topicRepository;
	private final TokenRepository tokenRepository;
	private final TopicTokenRepository topicTokenRepository;
	private final TopicMemberRepository topicMemberRepository;
	private final MemberRepository memberRepository;
	private final FCMService fcmService;
	private final TopicMemberBulkRepository topicMemberBulkRepository;
	private final TopicTokenBulkRepository topicTokenBulkRepository;
	private final TopicLogger topicLogger;

	// 토큰 만료 기간 상수 정의
	private static final int TOKEN_EXPIRATION_WEEKS = 10;

	// 토픽 구독 - 토픽 하나씩
	@Transactional
	public void subscribeToTopics(TopicRequest topicRequest) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		String topicName = topicRequest.getTopic();

		Topic topic = topicRepository.findByDepartment(topicName)
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

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
			.build();
		topicMemberBulkRepository.saveAll(List.of(topicMember));

		// 토픽과 토큰을 매핑하여 저장 -> 사용자가 가지고 있는 토큰들이 topic을 구독
		List<TopicToken> topicTokens = memberTokens.stream()
			.map(token -> new TopicToken(topic, token))
			.collect(Collectors.toList());
		topicTokenBulkRepository.saveAll(topicTokens);

		// FCM 서비스를 사용하여 토픽에 대한 구독 진행
		List<String> tokenValues = memberTokens.stream()
			.map(Token::getTokenValue)
			.collect(Collectors.toList());
		fcmService.subscribeToTopic(topicName, tokenValues);
	}

	// 토픽 구독 취소 - 토픽 하나씩
	@Transactional
	public void unsubscribeFromTopics(TopicRequest topicRequest) {
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		String topicName = topicRequest.getTopic();

		Topic topic = topicRepository.findByDepartment(topicName)
			.orElseThrow(() -> new CustomException(CustomErrorCode.TOPIC_NOT_FOUND));
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		topicLogger.log(topic.getDepartment() + "토픽 구독 취소");
		topicLogger.log("멤버 이메일 : " + memberEmail);

		// 멤버가 구독하고 있는 해당 토픽을 찾아서 삭제
		topicMemberRepository.deleteByTopicAndMember(topic, member);
		// 해당 토픽을 구독하는 모든 TopicToken 삭제
		topicTokenRepository.deleteByTopic(topic);
		List<Token> memberTokens = member.getTokens();

		// FCM 서비스를 사용하여 토픽에 대한 구독 취소 진행
		List<String> tokenValues = memberTokens.stream()
			.map(Token::getTokenValue)
			.collect(Collectors.toList());
		fcmService.unsubscribeFromTopic(topicName, tokenValues);
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
				.build();
			log.info("DB에 저장하는 token : " + token.getTokenValue());
			tokenRepository.save(token);

			// 사용자가 구독 중인 모든 토픽을 가져옴
			List<TopicMember> topicMembers = topicMemberRepository.findByMember(member);
			List<Topic> subscribedTopics = topicMembers.stream()
				.map(TopicMember::getTopic)
				.distinct()
				.collect(Collectors.toList());

			// 새 토큰을 기존에 구독된 모든 토픽과 매핑하여 TopicToken 생성 및 저장
			List<TopicToken> newSubscriptions = subscribedTopics.stream()
				.map(topic -> new TopicToken(topic, token))
				.collect(Collectors.toList());
			topicTokenRepository.saveAll(newSubscriptions);

			// 각 토픽에 대해 새 토큰 구독 처리
			for (Topic topic : subscribedTopics) {
				fcmService.subscribeToTopic(topic.getDepartment(), Collections.singletonList(token.getTokenValue()));
				log.info("새 토큰으로 " + topic.getDepartment() + " 토픽을 다시 구독합니다.");
			}
		}
	}

	// 매일 00:00(자정)에 트리거됩니다(0 0 0 * * ?). 따라서 하루에 한 번 작업이 실행됩니다.
	// 매일 자정에 실행되는 스케줄링 작업
	@Scheduled(cron = "0 0 0 * * ?")
	@Transactional
	public void unsubscribeExpiredTokens() {
		LocalDate now = LocalDate.now();
		log.info("오늘의 날짜 : " + now);

		// 만료된 토큰을 가져옵니다.
		List<Token> expiredTokens = tokenRepository.findByExpirationDate(now);

		// 만료된 토큰과 관련된 모든 TopicToken을 찾음
		List<TopicToken> topicTokens = topicTokenRepository.findByTokenIn(expiredTokens);

		// 만료된 토큰의 값들을 추출
		List<String> tokenValues = expiredTokens.stream()
			.map(Token::getTokenValue)
			.collect(Collectors.toList());

		// 각 TopicToken에 대해 구독 해지
		topicTokens.forEach(topicToken -> {
			fcmService.unsubscribeFromTopic(topicToken.getTopic().getDepartment(), tokenValues);
		});

		// 만료된 토큰 삭제
		topicTokenRepository.deleteAll(topicTokens); // TopicTokenRepository에서 먼저 삭제하고 TokenRepository에서 삭제
		tokenRepository.deleteAll(expiredTokens);
	}

	@Transactional
	public void resetAllSubscriptions() {
		topicLogger.log("로그인한 사용자의 구독 목록 초기화");
		// 스프링시큐리티 컨텍스트에서 유저 email 정보를 가져옴
		String email = SecurityContextHolder.getContext().getAuthentication().getName();

		// Member 객체를 가져온 뒤
		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// Member 객체를 가져온 뒤
		// Member member = memberRepository.findByEmailWithSubscriptions(email)
		// 	.orElseThrow(() -> new NoSuchElementException("Member not found for email: " + email));

		// Member가 구독하고 있는 Topic과 Member가 가지고 있는 토큰을 가져옴
		List<TopicMember> topicMembers = topicMemberRepository.findByMember(member);
		List<Token> tokens = tokenRepository.findByMember(member);

		// List<TopicMember> topicMembers = member.getTopicMembers();
		// List<Token> tokens = member.getTokens();

		List<String> tokenValues = tokens.stream()
			.map(Token::getTokenValue)
			.toList();

		// 사용자 구독하고 있는 topic 로그 출력
		topicLogger.log(email + " 가 구독하고 있는 토픽 목록 : ");
		topicMembers.forEach(topicMember -> {
			topicLogger.log("Topic: " + topicMember.getTopic().getDepartment());
			System.out.println(topicMember);
		});

		// FcmService를 호출해서 Member가 가지고 있는 Token과 Member가 구독하고 있는 Topic을 1대1로 매핑하여 구독 취소
		// TopicMemberRepository, TopicTokenRepository에서도 삭제
		topicMembers.forEach(topicMember -> {
			fcmService.unsubscribeFromTopic(topicMember.getTopic().getDepartment(), tokenValues);
			// topicTokenRepository.deleteByTopic(topicMember.getTopic());
			topicLogger.log("Deleting TopicMember - Member: " + topicMember.getMember().getEmail() + ", Topic: "
				+ topicMember.getTopic().getDepartment());
		});

		for (TopicMember topicMember : topicMembers) {
			System.out.println(topicMember);
		}

		// Extract Topic IDs from TopicMembers
		List<Long> topicIds = topicMembers.stream()
			.map(tm -> tm.getTopic().getId())
			.collect(Collectors.toList());

		// Delete all TopicTokens associated with these topics
		topicTokenRepository.deleteAllByIds(topicIds);

		// Extract TopicMember IDs
		List<Long> topicMemberIds = topicMembers.stream()
			.map(TopicMember::getId)
			.collect(Collectors.toList());

		// Delete all TopicMembers by IDs
		topicMemberRepository.deleteAllByIds(topicMemberIds);

	}

	@Transactional
	public TopicResponse getSubscribedTopics() {
		log.info("getSubscribedTopics 입장");
		// 스프링 시큐리티 컨텍스트에서 현재 사용자의 이메일 가져오기
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		log.info("가져온 이메일 : " + memberEmail);
		// 이메일을 기반으로 회원 정보 조회
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		// 회원이 구독하는 토픽 목록 조회
		List<TopicMember> topicMembers = topicMemberRepository.findByMember(member);

		// TopicMember 목록에서 토픽의 이름만 추출하여 반환
		List<String> topics = topicMembers.stream()
			.map(topicMember -> topicMember.getTopic().getKoreanTopic())
			.collect(Collectors.toList());


		// TopicResponse 객체 생성하여 반환
		return new TopicResponse(topics);
	}

	// 전체 topic 조회
	public TopicResponse getAllTopics() {
		List<Topic> topics = topicRepository.findAll();
		List<String> topicName = topics.stream()
			.map(Topic::getKoreanTopic)
			.toList();

		return new TopicResponse(topicName);
	}

	// 사용자가 구독하고 있는 토픽 상태 조회
	public List<TopicStatus> getTopicWithUserSubscriptionsStatus(Principal principal) {
		List<Topic> allTopics = topicRepository.findAll();
		Member member = memberRepository.findByEmail(principal.getName())
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		List<TopicMember> subscriptions = topicMemberRepository.findByMember(member);
		Set<Long> subscribedTopicIds = subscriptions.stream()
			.map(subscription -> subscription.getTopic().getId())
			.collect(Collectors.toSet());

		return allTopics.stream()
			.map(topic -> new TopicStatus(topic, subscribedTopicIds.contains(topic.getId())))
			.collect(Collectors.toList());
	}
}
