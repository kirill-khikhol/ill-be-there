package com.illbethere.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads .env before application.yml so ${GOOGLE_CLIENT_ID} is not resolved to empty.
 */
public class DotEnvProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        DotEnv.LoadResult result = DotEnv.applyToSystemProperties();
        if (result.values().isEmpty()) {
            return;
        }
        Map<String, Object> values = new HashMap<>(result.values());
        String clientId = result.values().get("GOOGLE_CLIENT_ID");
        String clientSecret = result.values().get("GOOGLE_CLIENT_SECRET");
        if (clientId != null && !clientId.isBlank()) {
            values.put("app.google.client-id", clientId);
        }
        if (clientSecret != null && !clientSecret.isBlank()) {
            values.put("app.google.client-secret", clientSecret);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("dotenv", values));
    }
}
