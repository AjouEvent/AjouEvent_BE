package com.example.ajouevent.service;

import com.example.ajouevent.dto.CalendarStoreDto;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
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
import org.springframework.beans.factory.annotation.Value;
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
    private static final List<String> SCOPES =
            Collections.singletonList(CalendarScopes.CALENDAR);

    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";


    private static final Pattern EMAIL_PATTERN = Pattern.compile("[^a-zA-Z0-9_-]");

    public void GoogleAPIClient(CalendarStoreDto calendarStoreDto, Principal principal) throws IOException, GeneralSecurityException {
        log.info("1");

        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

        Calendar service =
                new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT, principal.getName()))
                        .setApplicationName(APPLICATION_NAME)
                        .build();
        String calendarId = principal.getName();

        log.info("2");
        log.info("calendarId : " + calendarId);

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
        System.out.printf("Event created: %s\n", event.getHtmlLink());

    }

    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT, String userId)
            throws IOException {

        // Load client secrets.
        InputStream in = CalendarService.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH));
        DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore(getSafeUserId(userId));

            GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                    HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                    .setCredentialDataStore(dataStore).build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize(userId);
    }

    private static String getSafeUserId(String userId) {
        return EMAIL_PATTERN.matcher(userId).replaceAll("_");
    }

}
