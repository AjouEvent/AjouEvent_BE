package com.example.ajouevent.dao;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.TopicToken;
import com.example.ajouevent.dto.MemberDto.LoginRequest;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TokenRepository;
import com.example.ajouevent.repository.TopicRepository;
import com.example.ajouevent.repository.TopicTokenRepository;
import com.example.ajouevent.service.FCMService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FCMTokenDao {

	private final StringRedisTemplate tokenRedisTemplate;
	private final TokenRepository tokenRepository;
	private final MemberRepository memberRepository;
	private final TopicTokenRepository topicTokenRepository;
	// private final FCMService fcmService;

	public void saveToken(LoginRequest loginRequest) {
		tokenRedisTemplate.opsForValue()
			.set(loginRequest.getEmail(), loginRequest.getFcmToken());
	}

	public void saveFCMToken(LoginRequest loginRequest) {
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
				.build();
			log.info("DB에 저장하는 token : " + token.getValue());
			tokenRepository.save(token);

			// tokenRepo에 저장하고 기존 멤버가 구독하고 있는 토픽 불러서 topic 다시 구독
			// 멤버가 이미 구독 중인 모든 토픽을 불러와 새 토큰으로 구독
			// List<TopicToken> subscriptions = topicTokenRepository.findByTokenMember(member);
			// List<String> topicNames = subscriptions.stream()
			// 	.map(subscription -> subscription.getTopic().getDepartment()) // Topic의 식별 가능한 필드(예: department)
			// 	.distinct()
			// 	.toList();


			// 각 토픽에 대해 새 토큰 구독 처리
			// for (String topicName : topicNames) {
			// 	fcmService.subscribeToTopic(topicName, Collections.singletonList(token.getValue()));
			// 	log.info("새 토큰으로 " + topicName + " 토픽을 다시 구독합니다.");
			// }
		}
	}

	public String getToken(String email) {
		return tokenRedisTemplate.opsForValue().get(email);
	}

	public void deleteToken(String email) {
		tokenRedisTemplate.delete(email);
	}

	public boolean hasKey(String email) {
		return tokenRedisTemplate.hasKey(email);
	}
}