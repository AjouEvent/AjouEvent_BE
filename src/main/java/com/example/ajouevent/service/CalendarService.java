package com.example.ajouevent.service;

import com.example.ajouevent.dto.CalendarStoreDto;
import com.example.ajouevent.exception.CustomErrorCode;
import com.example.ajouevent.exception.CustomException;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
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
import com.google.auth.oauth2.GoogleCredentials;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.security.GeneralSecurityException;
import java.security.Principal;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class CalendarService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "ajouevent";
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES =
            Collections.singletonList(CalendarScopes.CALENDAR);

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String clientSecret;


    public void GoogleAPIClient(CalendarStoreDto calendarStoreDto, Principal principal) throws IOException, GeneralSecurityException {
        log.info("1");
        String DATA_DIRECTORY_PATH = "tokens/"+principal.getName();
        String CREDENTIAL_FILENAME = "StoredCredential";
        String keyFileName = "/credentials.json";
        InputStream in = CalendarService.class.getResourceAsStream(keyFileName);
        if (in == null) {
            throw new CustomException(CustomErrorCode.FILE_NOT_FOUND);
        }

        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

        FileDataStoreFactory dataStoreFactory = new FileDataStoreFactory(new File(DATA_DIRECTORY_PATH));
        DataStore<StoredCredential> dataStore = dataStoreFactory.getDataStore(CREDENTIAL_FILENAME);
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT,JSON_FACTORY, clientSecrets, SCOPES)
                .setCredentialDataStore(dataStore).build();
        Credential credential = flow.loadCredential(principal.getName());

        Calendar service = new com.google.api.services.calendar.Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME).build();
        String calendarId = principal.getName();

        log.info("2");
        /*
         * 캘린더 일정 생성
         */
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

}
