package com.illbethere.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class GoogleOAuthStartupLog {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthStartupLog.class);

    public GoogleOAuthStartupLog(AppProperties properties) {
        if (properties.isGoogleConfigured()) {
            log.info("Google OAuth enabled, clientId ends with {}", properties.googleClientIdHint());
        } else {
            log.warn("Google OAuth disabled. Set GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET "
                    + "in backend/.env or as environment variables, then restart.");
        }
    }
}
