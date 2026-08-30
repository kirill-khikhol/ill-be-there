package com.illbethere.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendOrigin = "http://localhost:5173";
    private String jwtSecret = "dev-only-change-me-use-at-least-32-chars!!";
    private String tokenEncryptKey = "dev-only-token-key-32-chars!!";
    private String osmImportToken = "dev-import";
    private String timezone = "Asia/Jerusalem";
    private final Google google = new Google();

    public String getFrontendOrigin() {
        return frontendOrigin;
    }

    public void setFrontendOrigin(String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public String getTokenEncryptKey() {
        return tokenEncryptKey;
    }

    public void setTokenEncryptKey(String tokenEncryptKey) {
        this.tokenEncryptKey = tokenEncryptKey;
    }

    public String getOsmImportToken() {
        return osmImportToken;
    }

    public void setOsmImportToken(String osmImportToken) {
        this.osmImportToken = osmImportToken;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public Google getGoogle() {
        return google;
    }

    public boolean isGoogleConfigured() {
        return google.getClientId() != null && !google.getClientId().isBlank();
    }

    public static class Google {
        private String clientId = "";
        private String clientSecret = "";

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }
    }
}
