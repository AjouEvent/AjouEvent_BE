package com.example.ajouevent.logger;

import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class WebhookLogger {

	private static final String LOG_FILE_PATH = "webhook_log.txt";

	public void log(String message) {
		try {
			FileWriter writer = new FileWriter(LOG_FILE_PATH, true);
			writer.write(LocalDateTime.now() + ": " + message + "\n");
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
