package com.example.ajouevent.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.google.firebase.FirebaseApp;
import com.google.firebase.ThreadManager;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CustomFcmThreadManager extends ThreadManager {

	private static final int THREAD_POOL_SIZE = 60;

	@Override
	protected ExecutorService getExecutor(FirebaseApp firebaseApp) {
		log.info("Creating custom FCM thread pool with {} threads", THREAD_POOL_SIZE);
		return Executors.newFixedThreadPool(THREAD_POOL_SIZE, getThreadFactory());
	}

	@Override
	protected void releaseExecutor(FirebaseApp firebaseApp, ExecutorService executorService) {
		log.info("Shutting down custom FCM thread pool");
		executorService.shutdownNow();
	}

	@Override
	protected ThreadFactory getThreadFactory() {
		return Executors.defaultThreadFactory();
	}
}

