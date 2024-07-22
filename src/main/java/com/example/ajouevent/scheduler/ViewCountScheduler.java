package com.example.ajouevent.scheduler;

import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.example.ajouevent.repository.EventRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ViewCountScheduler {

	private final StringRedisTemplate stringRedisTemplate;
	private final EventRepository eventRepository;

	@Transactional
	@Scheduled(cron = "0 0/3 * * * *") // 3분마다 실행
	public void updateDBFromRedis() {
		ScanOptions options = ScanOptions.scanOptions()
			.match("ClubEvent:views:*")
			.count(10)
			.build();
		Cursor<byte[]> cursor = stringRedisTemplate.executeWithStickyConnection(
			connection -> connection.scan(options)
		);
		if (cursor != null) {
			while (cursor.hasNext()) {
				String key = new String(cursor.next());
				Long eventId = Long.parseLong(key.split(":")[2]);
				String view = stringRedisTemplate.opsForValue().get(key);
				if (view != null) {
					eventRepository.updateViews(Long.valueOf(view), eventId);
					stringRedisTemplate.delete(key);
				}
			}
		}
	}
}