package com.example.ajouevent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicToken;
import com.example.ajouevent.dto.TopicRequest;
import com.example.ajouevent.repository.TokenRepository;
import com.example.ajouevent.repository.TopicRepository;
import com.example.ajouevent.repository.TopicTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.TopicManagementResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TopicService {

	private final TopicRepository topicRepository;
	private final TokenRepository tokenRepository;
	private final TopicTokenRepository topicTokenRepository;
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

}
