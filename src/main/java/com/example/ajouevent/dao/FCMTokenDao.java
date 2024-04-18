package com.example.ajouevent.dao;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import com.example.ajouevent.dto.MemberDto.LoginRequest;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class FCMTokenDao {

	private final StringRedisTemplate tokenRedisTemplate;

	public void saveToken(LoginRequest loginRequest) {
		tokenRedisTemplate.opsForValue()
			.set(loginRequest.getEmail(), loginRequest.getFcmToken());
	}

	public String getToken(String email) {
		return tokenRedisTemplate.opsForValue().get(email);
	}

	public void deleteToken(String email) {
		tokenRedisTemplate.delete(email);
	}

	public boolean hasKey(String email) {
		return tokenRedisTemplate.hasKey(email);
	}
}