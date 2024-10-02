package com.example.ajouevent.service;

import com.example.ajouevent.dto.CalendarStoreDto;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
public class CalendarService {

	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static final String APPLICATION_NAME = "ajouevent";
	private static final String TOKENS_DIRECTORY_PATH = "tokens";

	// 캘린더 모든 권한 요청
	private static final List<String> SCOPES =
		Collections.singletonList(CalendarScopes.CALENDAR);
	private static final String CREDENTIALS_FILE_PATH = "/credentials.json";
	private static final Pattern EMAIL_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");

	public void GoogleAPIClient(CalendarStoreDto calendarStoreDto, Principal principal) throws IOException, GeneralSecurityException {
		GoogleAuthorizationCodeFlow flow = getFlow(principal.getName());

		Credential credential = flow.loadCredential(getSafeUserId(principal.getName()));

		Calendar service = new Calendar.Builder(
			GoogleNetHttpTransport.newTrustedTransport(), JSON_FACTORY, credential)
			.setApplicationName(APPLICATION_NAME)
			.build();

		String calendarId = principal.getName();

		// 캘린더 일정 생성
		Event event = new Event()
			.setSummary(calendarStoreDto.getSummary()) // 일정 이름
			.setDescription(calendarStoreDto.getDescription()); // 일정 설명

		DateTime startDateTime = new DateTime(calendarStoreDto.getStartDate());
		EventDateTime start = new EventDateTime()
			.setDateTime(startDateTime);
		event.setStart(start);
		DateTime endDateTime = new DateTime(calendarStoreDto.getEndDate());
		EventDateTime end = new EventDateTime()
			.setDateTime(endDateTime);
		event.setEnd(end);

		//이벤트 실행
		event = service.events().insert(calendarId, event).execute();
		log.info("Event created: %s\n" + event.getHtmlLink());

	}

	public static void getCredentials(TokenResponse tokenResponse, String userId)
		throws IOException, GeneralSecurityException {

		GoogleAuthorizationCodeFlow flow = getFlow(userId);
		flow.createAndStoreCredential(tokenResponse, getSafeUserId(userId));
	}

	private static GoogleAuthorizationCodeFlow getFlow (String userId) throws IOException, GeneralSecurityException {
		InputStream in = CalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);

		if (in == null) {
			throw new CustomException(CustomErrorCode.FILE_NOT_FOUND);
		}

		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH));
		DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore(getSafeUserId(userId));

		return  new GoogleAuthorizationCodeFlow.Builder(
			GoogleNetHttpTransport.newTrustedTransport(),
			JSON_FACTORY,
			clientSecrets,
			SCOPES) // 모든 권한 요청
			.setCredentialDataStore(dataStore)
			.setAccessType("offline")
			.build();
	}

	private static String getSafeUserId(String userId) {
		return EMAIL_PATTERN.matcher(userId).replaceAll("_");
	}

}