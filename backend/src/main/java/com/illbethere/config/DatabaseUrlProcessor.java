package com.illbethere.config;

import java.net.URI;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Render and similar PaaS expose DATABASE_URL as postgres://user:pass@host/db.
 * Spring Boot expects a JDBC URL plus separate username/password.
 */
public class DatabaseUrlProcessor implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = firstNonBlank(
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("database.url"));
        if (raw == null || raw.startsWith("jdbc:")) {
            return;
        }
        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) {
            return;
        }
        try {
            URI uri = URI.create(raw.replaceFirst("^postgres(ql)?://", "http://"));
            String userInfo = uri.getUserInfo();
            if (userInfo == null || !userInfo.contains(":")) {
                return;
            }
            int split = userInfo.indexOf(':');
            String username = userInfo.substring(0, split);
            String password = userInfo.substring(split + 1);
            int port = uri.getPort() > 0 ? uri.getPort() : 5432;
            String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getPath();
            boolean managed = uri.getHost() != null
                    && (uri.getHost().contains("render.com") || uri.getHost().startsWith("dpg-"));
            if (managed && !jdbc.contains("sslmode")) {
                jdbc += "?sslmode=require";
            }
            Map<String, Object> props = new HashMap<>();
            props.put("spring.datasource.url", jdbc);
            props.put("spring.datasource.username", username);
            props.put("spring.datasource.password", password);
            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
        } catch (Exception ignored) {
            // keep default local datasource
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
