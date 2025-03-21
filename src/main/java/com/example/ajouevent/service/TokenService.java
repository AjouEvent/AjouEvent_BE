package com.example.ajouevent.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.domain.Topic;
import com.example.ajouevent.dto.MemberDto;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.example.ajouevent.logger.FcmTokenValidationLogger;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.TokenRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {
	private final TokenSubscriptionService tokenSubscriptionService;
	private final TopicQueryService topicQueryService;
	private final KeywordQueryService keywordQueryService;
	private final TokenRepository tokenRepository;
	private final MemberRepository memberRepository;
	private final FcmTokenValidationLogger fcmTokenValidationLogger;

	private static final int TOKEN_EXPIRATION_WEEKS = 4; // 만료 기간 설정

	// 만료된 토큰 정리 (매일 5AM 실행)
	@Scheduled(cron = "0 0 5 * * ?")
	@Transactional
	public void unsubscribeExpiredTokens() {
		fcmTokenValidationLogger.log("==== 만료된 FCM 토큰 정리 프로세스 시작 ====");
		LocalDate now = LocalDate.now();

		// 만료된 토큰 조회
		List<Token> expiredTokens = tokenRepository.findByExpirationDate(now);
		if (expiredTokens.isEmpty()) {
			fcmTokenValidationLogger.log("만료된 토큰이 없습니다.");
			return;
		}

		// 만료된 토큰 ID 목록
		List<Long> expiredTokenIds = expiredTokens.stream()
			.map(Token::getId)
			.collect(Collectors.toList());

		tokenSubscriptionService.unsubscribeTokensFromTopicsByTokenIds(expiredTokenIds);
		tokenSubscriptionService.unsubscribeTokensFromKeywordsByTokenIds(expiredTokenIds);

		tokenRepository.deleteAllByTokenIds(expiredTokenIds);
		fcmTokenValidationLogger.log("만료된 토큰의 개수:" + expiredTokens.size());
		fcmTokenValidationLogger.log("==== 만료된 FCM 토큰 정리 프로세스 완료 ====");
	}

	// 사용자의 새 FCM Token을 등록 또는 갱신하고 기존 구독 정보와 매핑
	@Transactional
	public void registerTokenWithSubscriptions(MemberDto.LoginRequest loginRequest) {
		Member member = memberRepository.findByEmail(loginRequest.getEmail())
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		Optional<Token> existingToken = tokenRepository.findByTokenValueAndMember(loginRequest.getFcmToken(), member);
		if (existingToken.isPresent()) {
			Token token = existingToken.get();
			token.setExpirationDate(LocalDate.now().plusWeeks(TOKEN_EXPIRATION_WEEKS));
			tokenRepository.save(token);
			return;
		}

		Token token = Token.builder()
			.tokenValue(loginRequest.getFcmToken())
			.member(member)
			.expirationDate(LocalDate.now().plusWeeks(TOKEN_EXPIRATION_WEEKS))
			.isDeleted(false)
			.build();
		tokenRepository.save(token);

		List<Topic> subscribedTopics = topicQueryService.getSubscribedTopics(member);
		List<Keyword> subscribedKeywords = keywordQueryService.getSubscribedKeywords(member);

		tokenSubscriptionService.subscribeTokensToTopics(subscribedTopics, token);
		tokenSubscriptionService.subscribeTokensToKeywords(subscribedKeywords, token);
	}
}
