package com.example.ajouevent.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.dto.TopicRequest;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TokenRepository;
import com.example.ajouevent.repository.TopicMemberRepository;
import com.example.ajouevent.repository.TopicRepository;
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

	@Transactional
	public void subscribeToTopic(TopicRequest topicRequest) {
		String topicName = topicRequest.getTopic();

		// 토픽 가져오기 또는 에러처리
		Topic topic = topicRepository.findByDepartment(topicName)
			.orElseThrow(() -> new NoSuchElementException("해당 토픽을 찾을 수 없습니다: " + topicName));

		// 사용자 정보는 스프링시큐리티 컨텍스트에서 가져옴
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		log.info("멤버 이메일 : " + memberEmail);

		// 현재 사용자의 토큰 목록 가져오기
		List<Token> memberTokens = tokenRepository.findByMemberEmail(memberEmail);

		// 현재 사용자 정보 가져오기
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new NoSuchElementException("해당 이메일의 멤버를 찾을 수 없습니다: " + memberEmail));

		// TopicMember 생성 후 Repository에 저장
		TopicMember topicMember = TopicMember.builder()
			.topic(topic)
			.member(member)
			.build();
		topicMemberRepository.save(topicMember);


		// 토픽과 토큰을 매핑하여 저장 -> 사용자가 가지고 있는 토큰들이 topic을 구독
		List<TopicToken> topicTokens = memberTokens.stream()
			.map(token -> new TopicToken(topic, token))
			.collect(Collectors.toList());
		topicTokenRepository.saveAll(topicTokens);

		// FCM 서비스를 사용하여 토픽에 대한 구독 진행
		List<String> tokenValues = memberTokens.stream()
			.map(Token::getValue)
			.collect(Collectors.toList());
		fcmService.subscribeToTopic(topicName, tokenValues);

	}

	@Transactional
	public void saveFCMToken(MemberDto.LoginRequest loginRequest) {
		log.info("saveFCMToken 메서드 호출");
		Member member = memberRepository.findByEmail(loginRequest.getEmail()).orElseThrow(NoSuchElementException::new);

		// Check if the token already exists
		Optional<Token> existingToken = tokenRepository.findByValueAndMember(loginRequest.getFcmToken(), member);
		if (existingToken.isPresent()) {
			log.info("이미 존재하는 토큰: " + existingToken.get().getValue());
		} else {
			// Only create and save a new token if it does not exist
			Token token = Token.builder()
				.value(loginRequest.getFcmToken())
				.member(member)
				.expirationDate(LocalDate.now().plusMonths(2))
				.build();
			log.info("DB에 저장하는 token : " + token.getValue());
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
				fcmService.subscribeToTopic(topic.getDepartment(), Collections.singletonList(token.getValue()));
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

		// 쿼리로 조회
		// @Query("SELECT tt FROM TopicToken tt JOIN FETCH tt.token t WHERE t.expirationDate < :now")
		// List<TopicToken> findSubscriptionsByExpiredTokens(@Param("now") LocalDateTime now);
	}

	@Transactional
	public void resetAllSubscriptions() {
		String email = SecurityContextHolder.getContext().getAuthentication().getName();
		Optional<Member> memberOptional = memberRepository.findByEmail(email);


		if (memberOptional.isPresent()) {
			Member member = memberOptional.get();
			List<TopicMember> topicMembers = topicMemberRepository.findByMember(member);
			List<Token> tokens = tokenRepository.findByMember(member);

			topicMembers.forEach(topicMember -> {
				fcmService.unsubscribeFromTopic(topicMember.getTopic().getDepartment(), tokens);
				topicTokenRepository.deleteByTopic(topicMember.getTopic());
				topicMemberRepository.delete(topicMember);
			});
		} else {
			// 멤버가 존재하지 않을 경우의 처리
			throw new RuntimeException();
		}
	}

	@Transactional
	public List<String> getSubscribedTopics() {
		log.info("getSubscribedTopics 입장");
		// 스프링 시큐리티 컨텍스트에서 현재 사용자의 이메일 가져오기
		String memberEmail = SecurityContextHolder.getContext().getAuthentication().getName();
		log.info("가져온 이메일 : " + memberEmail);
		// 이메일을 기반으로 회원 정보 조회
		Member member = memberRepository.findByEmail(memberEmail)
			.orElseThrow(() -> new NoSuchElementException("해당 이메일을 가진 사용자가 없습니다."));

		// 회원이 구독하는 토픽 목록 조회
		List<TopicMember> topicMembers = topicMemberRepository.findByMember(member);
		// TopicMember 목록에서 토픽의 이름만 추출하여 반환
		return topicMembers.stream()
			.map(topicMember -> topicMember.getTopic().getDepartment())
			.collect(Collectors.toList());
	}



}
