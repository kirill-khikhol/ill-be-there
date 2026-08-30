package com.illbethere.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.UserCredentials;
import com.illbethere.config.AppProperties;
import com.illbethere.domain.AppUser;
import com.illbethere.domain.Location;
import com.illbethere.security.TokenEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    private final AppProperties properties;
    private final TokenEncryptor tokenEncryptor;

    public GoogleCalendarService(AppProperties properties, TokenEncryptor tokenEncryptor) {
        this.properties = properties;
        this.tokenEncryptor = tokenEncryptor;
    }

    public String createEvent(AppUser user, Location location, Instant slotStart) {
        if (!properties.isGoogleConfigured() || user.getEncryptedRefreshToken() == null) {
            return null;
        }
        try {
            Calendar calendar = client(user);
            ZoneId zone = ZoneId.of(properties.getTimezone());
            ZonedDateTime start = slotStart.atZone(zone);
            ZonedDateTime end = start.plusMinutes(30);
            Event event = new Event()
                    .setSummary("I'll Be There: " + location.getName())
                    .setDescription("Обещание прийти на площадку в I'll Be There")
                    .setLocation(location.getLatitude() + ", " + location.getLongitude());
            event.setStart(new EventDateTime()
                    .setDateTime(new DateTime(start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                    .setTimeZone(properties.getTimezone()));
            event.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)))
                    .setTimeZone(properties.getTimezone()));
            Event created = calendar.events().insert("primary", event).execute();
            return created.getId();
        } catch (Exception e) {
            log.warn("Could not create Google Calendar event for user {}: {}", user.getId(), e.getMessage());
            return null;
        }
    }

    public void deleteEvent(AppUser user, String eventId) {
        if (eventId == null || eventId.isBlank() || user.getEncryptedRefreshToken() == null) {
            return;
        }
        try {
            client(user).events().delete("primary", eventId).execute();
        } catch (Exception e) {
            log.warn("Could not delete Google Calendar event {}: {}", eventId, e.getMessage());
        }
    }

    private Calendar client(AppUser user) throws Exception {
        String refreshToken = tokenEncryptor.decrypt(user.getEncryptedRefreshToken());
        UserCredentials credentials = UserCredentials.newBuilder()
                .setClientId(properties.getGoogle().getClientId())
                .setClientSecret(properties.getGoogle().getClientSecret())
                .setRefreshToken(refreshToken)
                .build();
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("I'll Be There")
                .build();
    }
}
