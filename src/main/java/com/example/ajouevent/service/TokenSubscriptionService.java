package com.example.ajouevent.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.KeywordToken;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.domain.TopicToken;
import com.example.ajouevent.repository.KeywordTokenBulkRepository;
import com.example.ajouevent.repository.KeywordTokenRepository;
import com.example.ajouevent.repository.TopicTokenBulkRepository;
import com.example.ajouevent.repository.TopicTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class TokenSubscriptionService {
	private final TopicTokenRepository topicTokenRepository;
	private final TopicTokenBulkRepository topicTokenBulkRepository;
	private final KeywordTokenRepository keywordTokenRepository;
	private final KeywordTokenBulkRepository keywordTokenBulkRepository;

	/** 특정 회원의 모든 Token 조회 */
	public List<Token> getTokensForMember(Member member) {
		return member.getTokens();
	}

	/** Topic 구독시 Token을 특정 Topic과 연결 */
	@Transactional
	public void subscribeTokenToTopic(Member member, Topic topic) {
		List<Token> tokens = getTokensForMember(member);
		List<TopicToken> topicTokens = tokens.stream()
			.map(token -> new TopicToken(topic, token))
			.toList();
		topicTokenBulkRepository.saveAll(topicTokens);
	}

	/** Keyword 구독 시 Token을 특정 Keyword와 연결 */
	@Transactional
	public void subscribeTokenToKeyword(Member member, Keyword keyword) {
		List<Token> tokens = getTokensForMember(member);
		List<KeywordToken> keywordTokens = tokens.stream()
			.map(token -> new KeywordToken(keyword, token))
			.toList();
		keywordTokenBulkRepository.saveAll(keywordTokens);
	}

	/** 토픽 구독 취소 시 Token 매핑 해제 */
	@Transactional
	public void unsubscribeTokenFromTopic(Member member, Topic topic) {
		List<Token> tokens = getTokensForMember(member);
		topicTokenRepository.deleteByTopicAndTokens(topic, tokens);
	}

	/** 키워드 구독 취소 시 Token 매핑 해제 */
	@Transactional
	public void unsubscribeTokenFromKeyword(Member member, Keyword keyword) {
		List<Token> tokens = getTokensForMember(member);
		keywordTokenRepository.deleteByKeywordAndTokens(keyword, tokens);
	}

	/** 사용자의 토큰을 모든 구독한 Topic과 매핑 */
	@Transactional
	public void subscribeTokensToTopics(List<Topic> topics, Token token) {
		List<TopicToken> topicTokens = topics.stream()
			.map(topic -> new TopicToken(topic, token))
			.toList();
		topicTokenBulkRepository.saveAll(topicTokens);
	}

	/** 사용자의 토큰을 모든 구독한 Keyword와 매핑 */
	@Transactional
	public void subscribeTokensToKeywords(List<Keyword> keywords, Token token) {
		List<KeywordToken> keywordTokens = keywords.stream()
			.map(keyword -> new KeywordToken(keyword, token))
			.toList();
		keywordTokenBulkRepository.saveAll(keywordTokens);
	}

	/** 특정 사용자의 모든 토큰과 모든 구독한 Topic과 매핑 해제 */
	@Transactional
	public void unsubscribeTokensFromAllTopics(Member member) {
		List<Token> tokens = getTokensForMember(member);
		if (tokens.isEmpty()) {
			return;
		}

		List<Long> tokenIds = tokens.stream()
			.map(Token::getId)
			.collect(Collectors.toList());
		topicTokenRepository.deleteAllByTokenIds(tokenIds);
	}

	/** 특정 사용자의 모든 토큰과 모든 구독한 Keyword와 매핑 해제 */
	@Transactional
	public void unsubscribeTokensFromAllKeywords(Member member) {
		List<Token> tokens = getTokensForMember(member);
		if (tokens.isEmpty()) {
			return;
		}

		List<Long> tokenIds = tokens.stream()
			.map(Token::getId)
			.collect(Collectors.toList());
		keywordTokenRepository.deleteAllByTokenIds(tokenIds);
	}

	/** 만료된 토큰들의 Topic 매핑 해제 */
	@Transactional
	public void unsubscribeTokensFromTopicsByTokenIds(List<Long> tokenIds) {
		if (tokenIds.isEmpty()) {
			return;
		}
		topicTokenRepository.deleteAllByTokenIds(tokenIds);
	}

	/** 만료된 토큰들의 Keyword 매핑 해제 */
	@Transactional
	public void unsubscribeTokensFromKeywordsByTokenIds(List<Long> tokenIds) {
		if (tokenIds.isEmpty()) {
			return;
		}
		keywordTokenRepository.deleteAllByTokenIds(tokenIds);
	}
}