package com.example.ajouevent.service;



import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
class MemberServiceTest {

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	// 저장 테스트

	@Test
	void testStrings() {
		//given
		ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
		String key = "first";

		//when
		valueOperations.set(key, "helloWorld!");

		//then
		String value = valueOperations.get(key);
		Assertions.assertThat(value).isEqualTo("helloWorld!");
	}

}