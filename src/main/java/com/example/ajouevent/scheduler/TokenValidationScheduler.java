package com.example.ajouevent.scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Token;
import com.example.ajouevent.logger.FcmTokenValidationLogger;
import com.example.ajouevent.repository.KeywordTokenRepository;
import com.example.ajouevent.repository.TokenBulkRepository;
import com.example.ajouevent.repository.TokenRepository;
import com.example.ajouevent.repository.TopicTokenRepository;
import com.example.ajouevent.service.FCMService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Component
public class TokenValidationScheduler {

	private static final int BATCH_SIZE = 400; // 배치 크기 설정
	private final TokenRepository tokenRepository;
	private final FCMService fcmService;
	private final TopicTokenRepository topicTokenRepository;
	private final KeywordTokenRepository keywordTokenRepository;
	private final FcmTokenValidationLogger fcmTokenValidationLogger;
	private final TokenBulkRepository tokenBulkRepository;

	@Scheduled(cron = "0 0 5 ? * SUN") // 매주 일요일 새벽 5시
	@Transactional
	public void validateAndRemoveInvalidTokens() {
		log.info("FCM 토큰 유효성 검사 및 삭제를 시작합니다.");

		fcmTokenValidationLogger.log("[START] FCM 토큰 유효성 검사 및 삭제 프로세스 시작");

		// 삭제 표시 되지 않은 토큰만 가져오기
		List<Token> tokens = tokenRepository.findByisDeletedFalse();
		fcmTokenValidationLogger.log("현재 저장된 활성 토큰 개수: " + tokens.size());

		// 토큰 값 추출
		List<String> tokenValues = tokens.stream()
			.map(Token::getTokenValue)
			.collect(Collectors.toList());

		// 배치 단위로 토큰 검증
		List<String> invalidTokens = batchValidateTokens(tokenValues);

		if (invalidTokens.isEmpty()) {
			fcmTokenValidationLogger.log("모든 토큰이 유효합니다. 삭제할 토큰 없음.");
			fcmTokenValidationLogger.log("[END] FCM 토큰 유효성 검사 종료");
			return;
		}

		fcmTokenValidationLogger.log("유효하지 않은 토큰 개수: " + invalidTokens.size());

		// 유효하지 않은 토큰과 관련된 Token 엔터티 찾기
		List<Token> invalidTokenEntities = tokens.stream()
			.filter(token -> invalidTokens.contains(token.getTokenValue()))
			.collect(Collectors.toList());

		// 무효 토큰 isDeleted 상태 업데이트
		markTokensAsDeleted(invalidTokenEntities);

		// // 무효 토큰 삭제
		// deleteInvalidTokensFromDB(invalidTokenEntities);
		fcmTokenValidationLogger.log("[END] FCM 토큰 유효성 검사 및 삭제 완료");
	}

	private List<String> batchValidateTokens(List<String> tokenValues) {
		List<String> invalidTokens = new ArrayList<>();
		int totalTokens = tokenValues.size();
		int batchCount = (int) Math.ceil((double) totalTokens / BATCH_SIZE);

		fcmTokenValidationLogger.log(" 총 " + totalTokens + "개의 토큰을 " + BATCH_SIZE + "개씩 배치 처리 (총 " + batchCount + "개 배치)");

		for (int i = 0; i < batchCount; i++) {
			int fromIndex = i * BATCH_SIZE;
			int toIndex = Math.min(fromIndex + BATCH_SIZE, totalTokens);
			List<String> batch = tokenValues.subList(fromIndex, toIndex);

			List<String> batchInvalidTokens = fcmService.validateTokens(batch);
			invalidTokens.addAll(batchInvalidTokens);

			fcmTokenValidationLogger.log(" 배치 " + (i + 1) + " 완료 | " + fromIndex + " ~ " + (toIndex - 1) + "번 토큰 검증 | 무효 토큰 수: " + batchInvalidTokens.size());
		}

		return invalidTokens;
	}

	private void markTokensAsDeleted(List<Token> invalidTokens) {
		invalidTokens.forEach(Token::markAsDeleted);
		tokenBulkRepository.updateTokens(invalidTokens);
		fcmTokenValidationLogger.log(invalidTokens.size() + "개의 유효하지 않은 토큰을 isDeleted=true로 업데이트 완료");
	}

	private void deleteInvalidTokensFromDB(List<Token> invalidTokens) {
		List<Long> invalidTokenIds = invalidTokens.stream()
			.map(Token::getId)
			.collect(Collectors.toList());

		fcmTokenValidationLogger.log("유효하지 않은 토큰 삭제 진행 중... (총 " + invalidTokenIds.size() + "개)");

		topicTokenRepository.deleteAllByTokenIds(invalidTokenIds);
		keywordTokenRepository.deleteAllByTokenIds(invalidTokenIds);
		tokenRepository.deleteAllByTokenIds(invalidTokenIds);

		fcmTokenValidationLogger.log(invalidTokenIds.size() + "개의 유효하지 않은 토큰 및 관련 데이터를 삭제 완료");
	}
}