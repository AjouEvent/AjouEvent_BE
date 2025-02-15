package com.example.ajouevent.logger;

import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

@Component
public class FcmTokenValidationLogger {
	private static final String LOG_FILE_PATH = "fcmTokenValidation_log.txt";

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
