package com.example.ajouevent.scheduler;

import com.example.ajouevent.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class CrawlingTokenScheduler {

	private final RedisService redisService;

	// 매일 새벽 5시 30분에 토큰을 생성하고 Redis에 저장
	@Scheduled(cron = "0 30 5 * * ?")
	public void scheduleTokenGeneration() {
		String key = "crawling-token";
		String token = redisService.generateAndStoreToken(key);
		log.info("Scheduled token generation: {}", token);
	}
}