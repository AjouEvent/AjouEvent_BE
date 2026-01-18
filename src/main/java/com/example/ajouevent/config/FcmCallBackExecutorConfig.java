package com.example.ajouevent.config;

import java.util.concurrent.Executor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class FcmCallBackExecutorConfig {

	@Bean("fcmCallbackExecutor")
	public Executor fcmCallbackExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(4);
		executor.setMaxPoolSize(10);
		executor.setThreadNamePrefix("fcm-callback-");
		executor.initialize();
		return executor;
	}
}