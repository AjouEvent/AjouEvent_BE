package com.example.ajouevent.service;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import com.example.ajouevent.domain.ClubEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Service
public class RedisService {
	private final Long clientAddressPostRequestWriteExpireDurationSec = 86400L;
	private final StringRedisTemplate stringRedisTemplate;

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

}