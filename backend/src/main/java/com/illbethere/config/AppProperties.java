package com.illbethere.config;

import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private String frontendOrigin = "http://localhost:5173";
    /** Path prefix when the SPA is not at domain root, e.g. /ill-be-there for GitHub Pages. */
    private String frontendBasePath = "";
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

    public String getFrontendBasePath() {
        return frontendBasePath;
    }

    public void setFrontendBasePath(String frontendBasePath) {
        this.frontendBasePath = frontendBasePath;
    }

    /** Browser Origin for CORS (scheme + host + port, no path). */
    public String corsOrigin() {
        try {
            URI uri = URI.create(trimSlash(frontendOrigin));
            if (uri.getScheme() == null || uri.getHost() == null) {
                return trimSlash(frontendOrigin);
            }
            StringBuilder origin = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() > 0) {
                origin.append(":").append(uri.getPort());
            }
            return origin.toString();
        } catch (Exception e) {
            return trimSlash(frontendOrigin);
        }
    }

    /** SPA root for OAuth redirects, including GitHub Pages project path. */
    public String frontendAppUrl() {
        String base = trimSlash(frontendOrigin);
        try {
            URI uri = URI.create(base);
            if (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath())) {
                return trimSlash(base);
            }
        } catch (Exception ignored) {
            // fall through to FRONTEND_BASE_PATH
        }
        String path = frontendBasePath == null ? "" : frontendBasePath.trim();
        if (path.isBlank() || "/".equals(path)) {
            return base;
        }
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return base + trimSlash(path);
    }

    private static String trimSlash(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
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
        return hasText(google.getClientId()) && hasText(google.getClientSecret());
    }

    public String googleClientIdHint() {
        String id = google.getClientId();
        if (!hasText(id) || id.length() < 8) {
            return "";
        }
        return "…" + id.substring(id.length() - 8);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
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
