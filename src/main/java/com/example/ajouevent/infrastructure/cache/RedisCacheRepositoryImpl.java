package com.example.ajouevent.infrastructure.cache;

import java.util.Optional;

import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
@Repository
public class RedisCacheRepositoryImpl implements CacheRepository {

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	@Override
	public <T> Optional<T> get(String key, TypeReference<T> typeRef) {
		try {
			String json = redisTemplate.opsForValue().get(key);
			if (json == null) {
				return Optional.empty();
			}
			return Optional.of(objectMapper.readValue(json, typeRef));
		} catch (Exception e) {
			log.error("🔌 Redis get 실패 (key: {}): {}", key, e.getMessage());
			return Optional.empty();
		}
	}

	@Override
	public void set(String key, Object value, long timeout, TimeUnit timeUnit) {
		if (value == null) { // null 캐싱 금지
			log.debug("⚠️ null 값은 캐싱하지 않음 (key: {})", key);
			return;
		}
		try {
			String json = objectMapper.writeValueAsString(value);
			redisTemplate.opsForValue().set(key, json, timeout, timeUnit);
		} catch (Exception e) {
			log.error("🔌 Redis set 실패 (key: {}): {}", key, e.getMessage());
		}
	}

	@Override
	public void delete(String key) {
		try {
			redisTemplate.delete(key);
		} catch (Exception e) {
			log.error("🔌 Redis delete 실패 (key: {}): {}", key, e.getMessage());
		}
	}
}