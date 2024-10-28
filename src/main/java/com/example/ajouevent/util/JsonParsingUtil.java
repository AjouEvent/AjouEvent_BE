package com.example.ajouevent.util;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import com.example.ajouevent.dto.SliceResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Repository
public class JsonParsingUtil {

	private final RedisTemplate<String, String> redisTemplate;
	private final ObjectMapper objectMapper;

	public String saveData(String key, Object object, long timeout, TimeUnit unit) {
		try {
			String jsonData = objectMapper.writeValueAsString(object);
			redisTemplate.opsForValue().set(key, jsonData, timeout, unit);
			return "Data saved successfully";
		} catch (JsonProcessingException e) {
			log.error("Error serializing object: ", e);
			throw new RuntimeException(e);
		}
	}

	public <T> Optional<SliceResponse> getSliceData(String key, Class<T> typeClass) {
		String jsonData = redisTemplate.opsForValue().get(key);

		try {
			if (StringUtils.hasText(jsonData)) {
				TypeReference<SliceResponse<T>> typeReference = new TypeReference<SliceResponse<T>>() {};
				return Optional.ofNullable(objectMapper.readValue(jsonData, typeReference));
			}
			return Optional.empty();
		} catch (JsonProcessingException e) {
			log.error("Error deserializing object: ", e);
			throw new RuntimeException(e);
		}
	}

	public void deleteData(String key) {
		redisTemplate.delete(key);
	}

	public <T> Optional<T> getData(String key, TypeReference<T> typeReference) {
		String json = redisTemplate.opsForValue().get(key);
		if (json == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(objectMapper.readValue(json, typeReference));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	public void clearCacheForType(String type) {
		Set<String> keys = redisTemplate.keys(type + ":*");
		if (keys != null) {
			redisTemplate.delete(keys);
		}
	}

	public void clearCache(String name) {
		Set<String> keys = redisTemplate.keys(name);
		if (keys != null) {
			redisTemplate.delete(keys);
		}
	}
}
