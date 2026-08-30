package com.illbethere.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DotEnv {

    public record LoadResult(Path file, Map<String, String> values) {
    }

    private DotEnv() {
    }

    public static LoadResult load() {
        Path file = findEnvFile();
        if (file == null) {
            return new LoadResult(null, Map.of());
        }
        try {
            List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
            if (!lines.isEmpty() && lines.get(0).startsWith("\uFEFF")) {
                lines.set(0, lines.get(0).substring(1));
            }
            return new LoadResult(file, parse(lines));
        } catch (Exception e) {
            System.err.println("I'll Be There: failed to read " + file.toAbsolutePath());
            return new LoadResult(file, Map.of());
        }
    }

    /**
     * Puts missing keys into system properties so ${GOOGLE_CLIENT_ID} resolves
     * even if application.yml is processed before EnvironmentPostProcessors.
     */
    public static LoadResult applyToSystemProperties() {
        LoadResult result = load();
        for (Map.Entry<String, String> entry : result.values().entrySet()) {
            if (isBlank(System.getenv(entry.getKey())) && isBlank(System.getProperty(entry.getKey()))) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        }
        String clientId = firstNonBlank(
                System.getenv("GOOGLE_CLIENT_ID"),
                System.getProperty("GOOGLE_CLIENT_ID"),
                result.values().get("GOOGLE_CLIENT_ID"));
        String clientSecret = firstNonBlank(
                System.getenv("GOOGLE_CLIENT_SECRET"),
                System.getProperty("GOOGLE_CLIENT_SECRET"),
                result.values().get("GOOGLE_CLIENT_SECRET"));
        if (!isBlank(clientId) && isBlank(System.getProperty("app.google.client-id"))) {
            System.setProperty("app.google.client-id", clientId);
        }
        if (!isBlank(clientSecret) && isBlank(System.getProperty("app.google.client-secret"))) {
            System.setProperty("app.google.client-secret", clientSecret);
        }
        return result;
    }

    static Path findEnvFile() {
        Path dir = Path.of(System.getProperty("user.dir", ".")).toAbsolutePath().normalize();
        for (int i = 0; i < 8 && dir != null; i++) {
            Path direct = dir.resolve(".env");
            if (Files.isRegularFile(direct)) {
                return direct;
            }
            Path nested = dir.resolve("backend").resolve(".env");
            if (Files.isRegularFile(nested)) {
                return nested;
            }
            dir = dir.getParent();
        }
        return null;
    }

    static Map<String, String> parse(List<String> lines) {
        Map<String, String> values = new LinkedHashMap<>();
        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                continue;
            }
            int split = line.indexOf('=');
            String key = line.substring(0, split).trim().replace("\uFEFF", "");
            String value = stripQuotes(line.substring(split + 1).trim());
            if (!key.isEmpty()) {
                values.put(key, value);
            }
        }
        return values;
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2
                && ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }
}
