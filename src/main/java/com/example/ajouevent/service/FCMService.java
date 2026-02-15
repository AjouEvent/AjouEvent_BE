package com.example.ajouevent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.example.ajouevent.domain.PushCluster;
import com.example.ajouevent.domain.PushClusterToken;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Token;
import com.example.ajouevent.logger.FcmTokenValidationLogger;
import com.example.ajouevent.logger.WebhookLogger;
import com.example.ajouevent.repository.PushClusterRepository;
import com.example.ajouevent.repository.PushClusterTokenBulkRepository;
import com.example.ajouevent.repository.PushClusterTokenRepository;
import com.example.ajouevent.repository.TokenBulkRepository;

import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMService {

	private final WebhookLogger webhookLogger;
	private final FcmTokenValidationLogger fcmTokenValidationLogger;
	private final PushClusterTokenRepository pushClusterTokenRepository;
	private final PushClusterTokenBulkRepository pushClusterTokenBulkRepository;
	private final PushClusterRepository pushClusterRepository;
	private final TokenBulkRepository tokenBulkRepository;

	private Message buildMessage(Long pushClusterId, PushClusterToken token, String title, String body, String imageUrl, String clickUrl, Map<Long, Long> unreadCountMap) {
		Long unreadCount = unreadCountMap.getOrDefault(token.getToken().getMember().getId(), 0L);

		return Message.builder()
			.setToken(token.getToken().getTokenValue())
			.setNotification(Notification.builder()
				.setTitle(title)
				.setBody(body)
				.setImage(imageUrl)
				.build())
			.putData("click_action", clickUrl)
			.putData("push_cluster_id", String.valueOf(pushClusterId))
			.putData("unread_count", String.valueOf(unreadCount))
			.build();
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void processPushResult(Long pushClusterId, List<PushClusterToken> clusterTokens, BatchResponse response) {
		PushCluster pushCluster = pushClusterRepository.findById(pushClusterId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.PUSH_CLUSTER_NOT_FOUND));

		int successCount = 0;
		int failCount = 0;
		List<Token> tokensToUpdate = new ArrayList<>();

		for (int i = 0; i < clusterTokens.size(); i++) {
			PushClusterToken pushClusterToken = clusterTokens.get(i);
			SendResponse sendResponse = response.getResponses().get(i);
			if (sendResponse.isSuccessful()) {
				pushClusterToken.markAsSuccess();
				successCount++;
			} else {
				MessagingErrorCode messagingErrorCode = sendResponse.getException().getMessagingErrorCode();
				pushClusterToken.markAsFail(messagingErrorCode.name()); // 실패 사유 기록
				failCount++;

				switch (messagingErrorCode) {
					case INTERNAL, UNAVAILABLE ->
						log.error("FCM 서버 내부 오류 발생 - 재시도 필요 (토큰 ID: {}, 오류 코드: {})", pushClusterToken.getId(), messagingErrorCode);
					case INVALID_ARGUMENT, UNREGISTERED -> {
						log.error("무효한 토큰 또는 등록 해제된 토큰 감지 - 토큰 삭제 처리 필요 (토큰 ID: {}, 오류 코드: {})", pushClusterToken.getId(), messagingErrorCode);
						Token token = pushClusterToken.getToken();
						token.markAsDeleted();
						tokensToUpdate.add(token);
					}
					case THIRD_PARTY_AUTH_ERROR, SENDER_ID_MISMATCH ->
						log.error("서버 설정/인증서 문제 발생 - 서버 확인 필요 (토큰 ID: {}, 오류 코드: {})", pushClusterToken.getId(), messagingErrorCode);
					case QUOTA_EXCEEDED ->
						log.error("FCM 할당량 초과 (토큰 ID: {}, 오류 코드: {})", pushClusterToken.getId(), messagingErrorCode);
					default -> log.error("기타 FCM 오류 발생: {} (토큰 ID: {})", messagingErrorCode, pushClusterToken.getId());
				}
			}
		}

		updatePushClusterTokens(clusterTokens);

		if (!tokensToUpdate.isEmpty()) {
			tokenBulkRepository.updateTokens(tokensToUpdate);
		}

		pushCluster.updateCountsAndStatus(successCount, failCount);
		pushClusterRepository.save(pushCluster);
		webhookLogger.log("푸시 완료 - PushClusterID: " + pushCluster.getId() + " 성공: " + successCount + " 실패: " + failCount);
	}

	// pushClusterToken 목록을 일괄 업데이트
	@Transactional
	public void updatePushClusterTokens(List<PushClusterToken> clusterTokens) {
		pushClusterTokenBulkRepository.updateAll(clusterTokens);
	}

	/**
	 * 리스트를 배치 단위로 나눔
	 */
	private <T> List<List<T>> splitIntoBatches(List<T> items, int batchSize) {
		List<List<T>> batches = new ArrayList<>();
		for (int i = 0; i < items.size(); i += batchSize) {
			int end = Math.min(i + batchSize, items.size());
			batches.add(items.subList(i, end));
		}
		return batches;
	}

	public void send(Message message) {
		FirebaseMessaging.getInstance().sendAsync(message);
	}

	public List<String> validateTokens(List<String> tokenValues) {
		if (tokenValues.isEmpty()) {
			return List.of(); // 토큰이 없으면 빈 리스트 반환
		}

		MulticastMessage message = MulticastMessage.builder()
			.addAllTokens(tokenValues)
			.build();

		List<String> invalidTokens = new ArrayList<>(); // 유효하지 않은 토큰 리스트
		List<String> validTokens = new ArrayList<>(); // 유효한 토큰 리스트

		try {
			BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(message); // validate_only=true

			// 응답 결과에서 성공/실패 토큰 분리
			for (int i = 0; i < response.getResponses().size(); i++) {
				if (response.getResponses().get(i).isSuccessful()) {
					validTokens.add(tokenValues.get(i));
				} else {
					invalidTokens.add(tokenValues.get(i));
				}
			}

			return invalidTokens; // 유효하지 않은 토큰만 반환
		} catch (FirebaseMessagingException e) {
			fcmTokenValidationLogger.log("FCM 토큰 유효성 검사 중 오류 발생" + e);

			// 예외 발생 시 재시도 로직 추가
			fcmTokenValidationLogger.log("예외 발생 - 재시도 로직 실행");
			retryValidation(tokenValues, invalidTokens, validTokens);

			fcmTokenValidationLogger.log("재시도 후 유효하지 않은 토큰 수: {}" + invalidTokens.size());
			return invalidTokens; // 최종 유효하지 않은 토큰 반환
		}
	}

	private void retryValidation(List<String> tokenValues, List<String> invalidTokens, List<String> validTokens) {
		// 재시도 로직
		for (String token : tokenValues) {
			try {
				// 단일 토큰에 대해 validate_only 요청
				Message singleMessage = Message.builder()
					.setToken(token)
					.build();

				FirebaseMessaging.getInstance().send(singleMessage); // validate_only=true
				validTokens.add(token); // 유효한 토큰으로 추가
			} catch (FirebaseMessagingException ex) {
				fcmTokenValidationLogger.log("재시도 중 실패한 토큰: {}" + token);
				invalidTokens.add(token); // 실패한 토큰은 무효로 처리
			}
		}
	}

	public void sendTopicPush(PushCluster cluster, Map<Long, Long> unreadCountMap) {
		// pushCluster에 연결된 토큰(회원/토큰 정보 포함)을 모두 조회
		List<PushClusterToken> tokens = pushClusterTokenRepository.findAllByPushClusterWithTokenAndMember(cluster);

		log.info("푸시 전송 대상 토큰 수: {}", tokens.size());

		// 클러스터 상태를 IN_PROGRESS 로 변경 (추후 통계에서 발송 상태 표시)
		cluster.markAsInProgress();

		// 400개씩 배치로 나누어 전송 (FCM sendEachAsync 권장 범위)
		List<List<PushClusterToken>> batches = splitIntoBatches(tokens, 400);

		for (List<PushClusterToken> batch : batches) {
			// 전송 직전 상태를 SENDING 으로 표시
			batch.forEach(PushClusterToken::markAsSending);  // 각 배치 전송 전 상태 변경

			// 상태값 변경을 일괄 반영
			pushClusterTokenBulkRepository.updateAll(batch);

			// 실제 FCM 메시지 구성
			List<Message> messages = batch.stream()
					.map(token -> buildMessage(cluster.getId(), token, cluster.getTitle(), cluster.getBody(), cluster.getImageUrl(), cluster.getClickUrl(), unreadCountMap))
					.collect(Collectors.toList());

			// FCM 비동기 발송
			ApiFuture<BatchResponse> responseFuture = FirebaseMessaging.getInstance().sendEachAsync(messages);

			// 발송 결과를 리스너(콜백)로 처리
			responseFuture.addListener(() -> {
				try {
					BatchResponse response = responseFuture.get();
					processPushResult(cluster.getId(), batch, response);
				} catch (InterruptedException e) {
					// 인터럽트 발생 시 현재 쓰레드 복구
					Thread.currentThread().interrupt();
					log.error("FCM 알림 비동기 처리 중 인터럽트 발생", e);

					// 해당 배치를 모두 실패로 처리
					batch.forEach(token -> token.markAsFail("INTERRUPTED_EXCEPTION"));
					updatePushClusterTokens(batch);
					webhookLogger.log("푸시 전송 중단 (인터럽트): pushClusterId=" + cluster.getId());
				} catch (ExecutionException e) {
					log.error("FCM 알림 비동기 처리 중 ExecutionException 발생", e);

					// 해당 배치를 모두 실패로 처리
					batch.forEach(token -> token.markAsFail("EXECUTION_EXCEPTION"));
					updatePushClusterTokens(batch);
					webhookLogger.log("푸시 전송 실패 (ExecutionException): pushClusterId=" + cluster.getId());
				}
			}, Runnable::run);
		}
	}

	public void sendKeywordPush(PushCluster cluster, Map<Long, Long> unreadCountMap) {
		sendTopicPush(cluster, unreadCountMap);
	}
}
