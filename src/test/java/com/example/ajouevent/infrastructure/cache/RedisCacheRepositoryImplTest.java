package com.example.ajouevent.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Optional;

import java.util.concurrent.TimeUnit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;

class RedisCacheRepositoryImplTest {

	private RedisCacheRepositoryImpl redisCacheRepositoryImpl;

	private RedisTemplate<String, String> redisTemplate;
	private ObjectMapper objectMapper;
	private ValueOperations<String, String> valueOperations;

	@BeforeEach
	void setUp() {
		redisTemplate = mock(RedisTemplate.class);
		objectMapper = spy(new ObjectMapper());
		valueOperations = mock(ValueOperations.class);

		when(redisTemplate.opsForValue()).thenReturn(valueOperations);

		redisCacheRepositoryImpl = new RedisCacheRepositoryImpl(redisTemplate, objectMapper);
	}

	@Test
	@DisplayName("캐시에서 값을 조회할 수 있다")
	void get() {
		// given
		String key = "testKey";
		String json = "\"testValue\""; // String 타입 JSON 직렬화 결과
		when(valueOperations.get(key)).thenReturn(json);

		// when
		Optional<String> result = redisCacheRepositoryImpl.get(key, new TypeReference<>() {});

		// then
		assertThat(result).isPresent().contains("testValue");
		verify(valueOperations).get(key);
	}

	@Test
	@DisplayName("캐시에 값을 저장할 수 있다")
	void set() throws Exception {
		// given
		String key = "testKey";
		String value = "testValue";
		String json = objectMapper.writeValueAsString(value);

		// when
		redisCacheRepositoryImpl.set(key, value, 60, TimeUnit.SECONDS);

		// then
		verify(valueOperations).set(key, json, 60, TimeUnit.SECONDS);
	}

	@Test
	@DisplayName("캐시에서 키를 삭제할 수 있다")
	void delete() {
		// given
		String key = "testKey";

		// when
		redisCacheRepositoryImpl.delete(key);

		// then
		verify(redisTemplate).delete(key);
	}

	@Test
	@DisplayName("Redis 연결 실패 시 get()은 Optional.empty()를 반환한다")
	void get_whenRedisFails_returnsEmptyOptional() {
		// given
		String key = "failKey";
		when(valueOperations.get(key)).thenThrow(new RedisConnectionFailureException("Redis down"));

		// when
		Optional<String> result = redisCacheRepositoryImpl.get(key, new TypeReference<>() {});

		// then
		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("Redis 연결 실패 시 set()은 예외를 던지지 않는다")
	void set_whenRedisFails_doesNotThrow() {
		// given
		String key = "failKey";
		String value = "value";
		doThrow(new RedisConnectionFailureException("Redis down")).when(valueOperations)
			.set(anyString(), anyString(), anyLong(), any());

		// when & then
		redisCacheRepositoryImpl.set(key, value, 30, TimeUnit.SECONDS); // 예외 없어야 성공
	}
}