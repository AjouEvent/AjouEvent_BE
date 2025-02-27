package com.example.ajouevent.scheduler;

import com.example.ajouevent.service.PushClusterService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PushClusterSyncScheduler {

	private final PushClusterService pushClusterService;

	@Scheduled(fixedRate = 60000) // 60초마다 실행
	public void syncMetricsToDatabase() {
		pushClusterService.syncMetricsToDatabase();
	}
}