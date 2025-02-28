package com.example.ajouevent.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import com.example.ajouevent.domain.Keyword;
import com.example.ajouevent.domain.PushCluster;
import com.example.ajouevent.domain.PushClusterToken;
import com.example.ajouevent.dto.UnreadNotificationCountDto;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.ajouevent.domain.Alarm;
import com.example.ajouevent.domain.AlarmImage;
import com.example.ajouevent.domain.Member;
import com.example.ajouevent.domain.Token;
import com.example.ajouevent.dto.NoticeDto;
import com.example.ajouevent.dto.ResponseDto;
import com.example.ajouevent.logger.AlarmLogger;
import com.example.ajouevent.logger.FcmTokenValidationLogger;
import com.example.ajouevent.logger.WebhookLogger;
import com.example.ajouevent.repository.MemberRepository;
import com.example.ajouevent.repository.PushClusterRepository;
import com.example.ajouevent.repository.PushClusterTokenBulkRepository;
import com.example.ajouevent.repository.PushClusterTokenRepository;
import com.example.ajouevent.repository.PushNotificationRepository;
import com.example.ajouevent.repository.TokenBulkRepository;

import com.google.api.core.ApiFuture;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMService {

	private final MemberRepository memberRepository;
	private final WebhookLogger webhookLogger;
	private final AlarmLogger alarmLogger;
	private final FcmTokenValidationLogger fcmTokenValidationLogger;

	private static final String DEFAULT_IMAGE_URL = "https://www.ajou.ac.kr/_res/ajou/kr/img/intro/img-symbol.png";
	private static final String REDIRECTION_URL_PREFIX = "https://www.ajouevent.com/event/";
	private static final String DEFAULT_CLICK_ACTION_URL =  "https://www.ajouevent.com";
	private final PushClusterTokenRepository pushClusterTokenRepository;
	private final PushClusterTokenBulkRepository pushClusterTokenBulkRepository;
	private final PushNotificationRepository pushNotificationRepository;
	private final PushClusterRepository pushClusterRepository;
	private final TokenBulkRepository tokenBulkRepository;

	public void sendAlarm(String email, Alarm alarm) {
		// 사용자 조회
		Member member = memberRepository.findByEmail(email)
			.orElseThrow(() -> new CustomException(CustomErrorCode.USER_NOT_FOUND));

		if (member.getTokens().isEmpty()) {
			alarmLogger.log("알림 전송 실패: 토큰이 없습니다.");
		}

		String title = alarm.getSubject(); // ex) 아주대학교 소프트웨어학과 공지사항
		String body = alarm.getTitle(); // ex) IT 해외 현장 연수
		String url = "https://ajou-event.shop"; // 실제 학교 홈페이지 공지사항으로 이동 (default는 우리 웹사이트)

		if (alarm.getUrl() != null) {
			url = alarm.getUrl();
		}

		// 기본 default 이미지는 학교 로고
		String imageUrl = "https://ajou-event-bucket.s3.ap-northeast-2.amazonaws.com/static/1e7b1dc2-ae1b-4254-ba38-d1a0e7cfa00c.20240307_170436.jpg";

		if (!alarm.getAlarmImageList().isEmpty()) { // S3에서 꺼내오기 or 크롤링으로
			AlarmImage firstImage = alarm.getAlarmImageList().get(0);
			imageUrl = firstImage.getUrl();
		}


		List<Token> tokens = member.getTokens();

		alarmLogger.log(email + " 에게 알림 전송");

		for (Token token : tokens) {
			Message message = Message.builder()
				.setToken(token.getTokenValue())
				.setNotification(Notification.builder()
					.setTitle(title)
					.setBody(body)
					.setImage(imageUrl)
					.build())
				.putData("click_action", url) // 동아리, 학생회 이벤트는 post한 이벤트 상세 페이지로 redirection "https://ajou-event.shop/event/{eventId}
				.build();
			alarmLogger.log(token + " 토큰으로 알림 전송");
			send(message);
		}

		alarmLogger.log("전송하는 알림: " + title);

		ResponseEntity.ok().body(ResponseDto.builder()
			.successStatus(HttpStatus.OK)
			.successContent("푸쉬 알림 성공")
			.Data(tokens)
			.build()
		);
	}

	@Transactional
	public void sendNoticeNotification(NoticeDto noticeDto, Long eventId, Long pushClusterId) {
		PushCluster pushCluster = pushClusterRepository.findById(pushClusterId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.PUSH_CLUSTER_NOT_FOUND));

		List<PushClusterToken> clusterTokens = pushClusterTokenRepository.findAllByPushClusterWithTokenAndMember(pushCluster);

		Map<Long, Long> unreadCountMap = pushNotificationRepository.countUnreadNotificationsForTopic(noticeDto.getKoreanTopic())
			.stream()
			.collect(Collectors.toMap(UnreadNotificationCountDto::getMemberId, UnreadNotificationCountDto::getUnreadNotificationCount));

		sendPushClusterNotification(
			clusterTokens,
			pushCluster,
			composeMessageTitle(noticeDto),
			composeBody(noticeDto),
			getFirstImageUrl(noticeDto),
			getRedirectionUrl(noticeDto, eventId),
			unreadCountMap
		);
	}

	@Transactional
	public void sendKeywordPushNotification(List<PushClusterToken> clusterTokens, PushCluster pushCluster, Keyword keyword, NoticeDto noticeDto, Long eventId) {
		Map<Long, Long> unreadCountMap = pushNotificationRepository.countUnreadNotificationsForKeyword(keyword.getEncodedKeyword())
			.stream()
			.collect(Collectors.toMap(UnreadNotificationCountDto::getMemberId, UnreadNotificationCountDto::getUnreadNotificationCount));

		sendPushClusterNotification(
			clusterTokens,
			pushCluster,
			keyword.getKoreanKeyword() + "-" + composeMessageTitle(noticeDto),
			composeBody(noticeDto),
			getFirstImageUrl(noticeDto),
			getRedirectionUrl(noticeDto, eventId),
			unreadCountMap
		);
	}

	public void sendPushClusterNotification(List<PushClusterToken> clusterTokens, PushCluster pushCluster, String title, String body, String imageUrl, String clickUrl, Map<Long, Long> unreadCountMap) {
		if (clusterTokens.isEmpty()) {
			webhookLogger.log("푸시 전송 스킵 - PushClusterID: " + pushCluster.getId() + " 알림 대상 토큰이 없습니다.");
			pushCluster.updateCountsAndStatus(0, 0);  // 대상 없음 처리
			pushClusterRepository.save(pushCluster);
			return;
		}

		pushCluster.markAsInProgress();

		List<List<PushClusterToken>> batches = splitIntoBatches(clusterTokens, 400);

		for (List<PushClusterToken> batch : batches) {
			batch.forEach(PushClusterToken::markAsSending);  // 각 배치 전송 전 상태 변경
			pushClusterTokenBulkRepository.saveAll(batch);
			List<Message> messages = batch.stream()
				.map(token -> buildMessage(pushCluster.getId(), token, title, body, imageUrl, clickUrl, unreadCountMap))
				.collect(Collectors.toList());

			ApiFuture<BatchResponse> responseFuture = FirebaseMessaging.getInstance().sendEachAsync(messages);

			responseFuture.addListener(() -> {
				try {
					BatchResponse response = responseFuture.get();
					processPushResult(pushCluster.getId(), batch, response);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();  // 인터럽트 복구
					log.warn("FCM 알림 비동기 처리 중 인터럽트 발생", e);
					batch.forEach(PushClusterToken::markAsFail);
					updatePushClusterTokens(batch);
					webhookLogger.log("푸시 전송 중단 (인터럽트): pushClusterId=" + pushCluster.getId());
				} catch (ExecutionException e) {
					log.error("FCM 알림 비동기 처리 중 ExecutionException 발생", e);
					batch.forEach(PushClusterToken::markAsFail);
					updatePushClusterTokens(batch);
					webhookLogger.log("푸시 전송 실패 (ExecutionException): pushClusterId=" + pushCluster.getId());
				}
			}, Runnable::run);
		}
	}

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

	@Transactional
	public void processPushResult(Long pushClusterId, List<PushClusterToken> clusterTokens, BatchResponse response) {
		PushCluster pushCluster = pushClusterRepository.findById(pushClusterId)
			.orElseThrow(() -> new CustomException(CustomErrorCode.PUSH_CLUSTER_NOT_FOUND));

		int successCount = 0;
		int failCount = 0;
		List<Token> tokensToUpdate = new ArrayList<>();

		for (int i = 0; i < clusterTokens.size(); i++) {
			PushClusterToken pushClusterToken = clusterTokens.get(i);
			if (response.getResponses().get(i).isSuccessful()) {
				pushClusterToken.markAsSuccess();
				successCount++;
			} else {
				pushClusterToken.markAsFail();
				failCount++;
				Token token = pushClusterToken.getToken();
				token.markAsDeleted();
				tokensToUpdate.add(token);
			}
		}

		updatePushClusterTokens(clusterTokens);

		if (!tokensToUpdate.isEmpty()) {
			tokenBulkRepository.updateTokens(tokensToUpdate);
		}

		pushCluster.updateCountsAndStatus(successCount, failCount);
		pushClusterRepository.save(pushCluster);
		webhookLogger.log("푸시 완료 - PushClusterID: " + pushClusterId + " 성공: " + successCount + " 실패: " + failCount);
	}

	// pushClusterToken 목록을 일괄 업데이트
	@Transactional
	public void updatePushClusterTokens(List<PushClusterToken> clusterTokens) {
		// ✅ 먼저 저장하여 ID가 생성되도록 함
		pushClusterTokenRepository.saveAll(clusterTokens);
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

	private boolean processTokenResponse(PushClusterToken tokenEntity, boolean isSuccess, List<Token> tokensToUpdate) {
		if (isSuccess) {
			tokenEntity.markAsSuccess();
			return true;
		} else {
			tokenEntity.markAsFail();
			Token token = tokenEntity.getToken();
			token.markAsDeleted();
			tokensToUpdate.add(token);
			return false;
		}
	}

	private String composeMessageTitle(NoticeDto noticeDto) {
		return String.format("[%s]", noticeDto.getKoreanTopic());
	}

	private String composeBody(NoticeDto noticeDto) {
		return noticeDto.getTitle();
	}

	private String getFirstImageUrl(NoticeDto noticeDto) {
		List<String> images = Optional.ofNullable(noticeDto.getImages())
			.filter(imgs -> !imgs.isEmpty())
			.orElseGet(() -> {
				List<String> defaultImages = new ArrayList<>();
				defaultImages.add(DEFAULT_IMAGE_URL);
				return defaultImages;
			});
		return images.get(0);
	}

	private String getRedirectionUrl(NoticeDto noticeDto, Long eventId) {
		String url = Optional.ofNullable(noticeDto.getUrl())
			.filter(u -> !u.isEmpty())
			.map(u -> REDIRECTION_URL_PREFIX + eventId) // 크롤링 후 DB에 저장된, 우리 앱 상세페이지로 이동
			.orElse(DEFAULT_CLICK_ACTION_URL);
		webhookLogger.log("리다이렉션하는 URL: " + url);
		return url;
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
}
