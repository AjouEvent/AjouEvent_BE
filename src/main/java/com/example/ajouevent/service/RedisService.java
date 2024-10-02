package com.example.ajouevent.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.example.ajouevent.domain.ClubEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisService {
	private static final Long clientAddressPostRequestWriteExpireDurationSec = 86400L;
	private final StringRedisTemplate stringRedisTemplate;
	private static final Long crawlingTokenExpireDurationSec = 90000L; // 토큰 TTL (1일+a)

	public boolean isFirstIpRequest(String clientAddress, Long eventId, Object reference) {
		String key = generateKey(clientAddress, eventId, reference);
		log.info("user post request key: {}", key);
		if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
			log.info("이미 조회한 user");
			return false;
		}
		return true;
	}

	public void writeClientRequest(String userId, Long eventId, Object reference) {
		String key = generateKey(userId, eventId, reference);
		log.debug("user post request key: {}", key);

		stringRedisTemplate.opsForValue().append(key, String.valueOf(eventId));
		stringRedisTemplate.expire(key, clientAddressPostRequestWriteExpireDurationSec, TimeUnit.SECONDS);
	}

	private String generateKey(String userId, Long eventId, Object reference) {
		String objectType;
		if (reference instanceof ClubEvent) {
			objectType = "ClubEvent";
		} else {
			objectType = "unknown";
		}
		return userId + "'s " + objectType + "Num - No." + eventId;
	}

	// Redis에 토큰을 저장하는 메서드
	public String generateAndStoreToken(String key) {
		// 랜덤 문자열 생성 (16자리)
		String randomString = RandomStringUtils.randomAlphanumeric(16);

		// Base64로 인코딩
		String token = Base64.getEncoder().encodeToString(randomString.getBytes(StandardCharsets.UTF_8));

		// Redis에 저장 (key는 토큰의 식별자)
		stringRedisTemplate.opsForValue().set(key, token, crawlingTokenExpireDurationSec, TimeUnit.SECONDS);

		log.info("Generated token: {} for key: {}", token, key);
		return token;
	}

	// Redis에서 토큰 유효성 검증
	public boolean isTokenValid(String key, String token) {
		String storedToken = stringRedisTemplate.opsForValue().get(key);
		return token.equals(storedToken);
	}


}