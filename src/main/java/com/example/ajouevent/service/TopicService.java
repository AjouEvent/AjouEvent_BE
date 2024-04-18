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

	public void subscribeToTopic(TopicRequest topicRequest) {
		String topicName = topicRequest.getTopic();
		List<Token> tokens = topicRequest.getTokens();
		Optional<Topic> optionalTopic = topicRepository.findByDepartment(topicName);
		Topic topic;
		if (optionalTopic.isPresent()) {
			topic = optionalTopic.get();
		} else {
			topic = Topic.builder()
				.department(topicName)
				.build();
		}
		topicRepository.save(topic);

		List<TopicToken> topicTokens = new ArrayList<>();
		for (Token token : tokens) {
			Token existingToken = tokenRepository.findByValue(token.getValue()); // 데이터베이스에서 토큰 값으로 조회
			if (existingToken != null) {
				// 이미 존재하는 토큰인 경우 기존 토큰을 사용
				topicTokens.add(TopicToken.builder()
					.topic(topic)
					.token(existingToken)
					.build());
			} else {
				// 존재하지 않는 경우 새로운 토큰을 저장
				Token savedToken = tokenRepository.save(token);
				topicTokens.add(TopicToken.builder()
					.topic(topic)
					.token(savedToken)
					.build());
			}
		}

		topicTokenRepository.saveAll(topicTokens);

		List<String> tokenValues = tokens.stream()
			.map(Token::getValue)
			.collect(Collectors.toList());

		fcmService.subscribeToTopic(topicName, tokenValues);

	}

}
