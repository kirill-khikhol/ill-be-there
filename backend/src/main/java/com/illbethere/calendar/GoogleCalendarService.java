package com.illbethere.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
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
import java.util.Date;
import java.util.TimeZone;

@Service
public class GoogleCalendarService {

    private static final Logger log = LoggerFactory.getLogger(GoogleCalendarService.class);

    public record WriteResult(String eventId, String warning) {
        public static WriteResult ok(String eventId) {
            return new WriteResult(eventId, null);
        }

        public static WriteResult fail(String warning) {
            return new WriteResult(null, warning);
        }
    }

    private final AppProperties properties;
    private final TokenEncryptor tokenEncryptor;

    public GoogleCalendarService(AppProperties properties, TokenEncryptor tokenEncryptor) {
        this.properties = properties;
        this.tokenEncryptor = tokenEncryptor;
    }

    public WriteResult createEvent(AppUser user, Location location, Instant slotStart) {
        if (!properties.isGoogleConfigured()) {
            return WriteResult.fail("google_not_configured");
        }
        if (!user.hasCalendarToken()) {
            return WriteResult.fail("no_calendar_access");
        }
        try {
            Calendar calendar = client(user);
            TimeZone tz = TimeZone.getTimeZone(ZoneId.of(properties.getTimezone()));
            Event event = new Event()
                    .setSummary("I'll Be There: " + location.getName())
                    .setDescription("Promise to show up at this pitch in I'll Be There")
                    .setLocation(location.getLatitude() + ", " + location.getLongitude());
            event.setStart(new EventDateTime()
                    .setDateTime(new DateTime(Date.from(slotStart), tz))
                    .setTimeZone(properties.getTimezone()));
            event.setEnd(new EventDateTime()
                    .setDateTime(new DateTime(Date.from(slotStart.plusSeconds(30 * 60)), tz))
                    .setTimeZone(properties.getTimezone()));
            Event created = calendar.events().insert("primary", event).execute();
            log.info("Created Google Calendar event {} for user {}", created.getId(), user.getId());
            return WriteResult.ok(created.getId());
        } catch (Exception e) {
            log.warn("Could not create Google Calendar event for user {}", user.getId(), e);
            String message = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            if (message.contains("accessNotConfigured") || message.contains("has not been used")
                    || message.contains("Calendar API")) {
                return WriteResult.fail("calendar_api_disabled");
            }
            return WriteResult.fail("calendar_error:" + message);
        }
    }

    public void deleteEvent(AppUser user, String eventId) {
        if (eventId == null || eventId.isBlank() || !user.hasCalendarToken()) {
            return;
        }
        try {
            client(user).events().delete("primary", eventId).execute();
        } catch (Exception e) {
            log.warn("Could not delete Google Calendar event {}: {}", eventId, e.getMessage());
        }
    }

    private Calendar client(AppUser user) throws Exception {
        GoogleCredentials credentials;
        if (user.getEncryptedRefreshToken() != null && !user.getEncryptedRefreshToken().isBlank()) {
            String refreshToken = tokenEncryptor.decrypt(user.getEncryptedRefreshToken());
            UserCredentials userCredentials = UserCredentials.newBuilder()
                    .setClientId(properties.getGoogle().getClientId())
                    .setClientSecret(properties.getGoogle().getClientSecret())
                    .setRefreshToken(refreshToken)
                    .build();
            userCredentials.refreshIfExpired();
            credentials = userCredentials;
        } else {
            String accessToken = tokenEncryptor.decrypt(user.getEncryptedAccessToken());
            Date expiry = user.getAccessTokenExpiresAt() != null
                    ? Date.from(user.getAccessTokenExpiresAt())
                    : null;
            credentials = GoogleCredentials.create(new AccessToken(accessToken, expiry));
        }
        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("I'll Be There")
                .build();
    }
}
