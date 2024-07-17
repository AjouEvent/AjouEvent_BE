package com.example.ajouevent.service;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.example.ajouevent.domain.ClubEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisService {
	private final Long clientAddressPostRequestWriteExpireDurationSec = 86400L;
	private final RedisTemplate<String, Object> redisTemplate;

	public boolean isFirstIpRequest(String clientAddress, Long eventId, Object reference) {
		String key = generateKey(clientAddress, eventId, reference);
		log.info("user post request key: {}", key);
		if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
			return false;
		}
		return true;
	}

	public void writeClientRequest(String userId, Long talentId, Object reference) {
		String key = generateKey(userId, talentId, reference);
		log.debug("user post request key: {}", key);

		redisTemplate.opsForValue().append(key, String.valueOf(talentId));
		redisTemplate.expire(key, clientAddressPostRequestWriteExpireDurationSec, TimeUnit.SECONDS);
	}

	private String generateKey(String userId, Long talentId, Object reference) {
		String objectType;
		if (reference instanceof ClubEvent) {
			objectType = "ClubEvent";
		} else {
			objectType = "unknown";
		}
		return userId + "'s " + objectType + "Num - No." + talentId;
	}

}