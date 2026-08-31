package com.illbethere.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Render exposes DATABASE_URL as postgres://user:pass@host/db.
 * Spring needs SPRING_DATASOURCE_URL as jdbc:postgresql://... plus user/password.
 * Applied from main() before Spring starts so application.yml placeholders resolve.
 */
public class DatabaseUrlProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER - 2;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        applyToSystemProperties();
        String jdbc = System.getProperty("SPRING_DATASOURCE_URL");
        String user = System.getProperty("SPRING_DATASOURCE_USERNAME");
        String password = System.getProperty("SPRING_DATASOURCE_PASSWORD");
        if (jdbc == null || jdbc.isBlank() || jdbc.contains("localhost")) {
            return;
        }
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", jdbc);
        if (user != null) {
            props.put("spring.datasource.username", user);
        }
        if (password != null) {
            props.put("spring.datasource.password", password);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
    }

    public static void applyToSystemProperties() {
        String raw = firstNonBlank(System.getenv("DATABASE_URL"), System.getProperty("DATABASE_URL"));
        if (raw != null) {
            raw = stripQuotes(raw.replace("\r", "").replace("\n", "").trim());
        }
        boolean onRender = firstNonBlank(System.getenv("RENDER"), System.getenv("RENDER_SERVICE_ID")) != null;
        System.out.println("I'll Be There: DATABASE_URL present=" + (raw != null && !raw.isBlank())
                + (raw == null || raw.isBlank()
                ? ""
                : " length=" + raw.length() + " starts=" + raw.substring(0, Math.min(18, raw.length()))));

        if (raw == null || raw.isBlank()) {
            Parsed fromParts = fromDiscreteEnv();
            if (fromParts != null) {
                applyParsed(fromParts);
                return;
            }
            if (onRender) {
                throw new IllegalStateException(
                        "DATABASE_URL is not set at runtime. Render → Environment: key DATABASE_URL, "
                                + "value = Internal Database URL (starts with postgresql://), available for Runtime.");
            }
            return;
        }

        if (raw.startsWith("jdbc:")) {
            System.setProperty("SPRING_DATASOURCE_URL", raw);
            copyIfPresent("PGUSER", "SPRING_DATASOURCE_USERNAME");
            copyIfPresent("PGPASSWORD", "SPRING_DATASOURCE_PASSWORD");
            System.out.println("I'll Be There: datasource using jdbc URL as provided");
            return;
        }

        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) {
            throw new IllegalStateException(
                    "DATABASE_URL must start with postgresql:// (got starts="
                            + raw.substring(0, Math.min(24, raw.length()))
                            + "). Use Add from Database → Connection String, not Host.");
        }

        applyParsed(parse(raw));
    }

    private static void applyParsed(Parsed parsed) {
        String jdbc = "jdbc:postgresql://" + parsed.host + ":" + parsed.port + "/" + parsed.database;
        if (parsed.query != null && !parsed.query.isBlank()) {
            jdbc += (jdbc.contains("?") ? "&" : "?") + parsed.query;
        } else if (parsed.host.contains("render.com") && !jdbc.contains("sslmode")) {
            jdbc += "?sslmode=require";
        }
        System.setProperty("SPRING_DATASOURCE_URL", jdbc);
        System.setProperty("SPRING_DATASOURCE_USERNAME", parsed.user);
        System.setProperty("SPRING_DATASOURCE_PASSWORD", parsed.password);
        System.out.println("I'll Be There: datasource host=" + parsed.host
                + " port=" + parsed.port + " db=" + parsed.database + " user=" + parsed.user);
    }

    private static Parsed fromDiscreteEnv() {
        String host = firstNonBlank(System.getenv("PGHOST"));
        String user = firstNonBlank(System.getenv("PGUSER"));
        String password = firstNonBlank(System.getenv("PGPASSWORD"));
        String database = firstNonBlank(System.getenv("PGDATABASE"));
        String portText = firstNonBlank(System.getenv("PGPORT"), "5432");
        if (host == null || user == null || password == null || database == null) {
            return null;
        }
        return new Parsed(host, Integer.parseInt(portText), database, user, password, null);
    }

    private static void copyIfPresent(String fromEnv, String toProperty) {
        String value = System.getenv(fromEnv);
        if (value != null && !value.isBlank()) {
            System.setProperty(toProperty, value);
        }
    }

    static Parsed parse(String raw) {
        String rest = raw.replaceFirst("^postgres(ql)?://", "");
        int at = rest.lastIndexOf('@');
        if (at < 1) {
            throw new IllegalArgumentException("DATABASE_URL is missing user@host");
        }
        String userInfo = rest.substring(0, at);
        String hostPart = rest.substring(at + 1);
        int colon = userInfo.indexOf(':');
        if (colon < 0) {
            throw new IllegalArgumentException("DATABASE_URL is missing password");
        }
        String user = decode(userInfo.substring(0, colon));
        String password = decode(userInfo.substring(colon + 1));

        String query = null;
        int q = hostPart.indexOf('?');
        if (q >= 0) {
            query = hostPart.substring(q + 1);
            hostPart = hostPart.substring(0, q);
        }

        String hostPort;
        String database;
        int slash = hostPart.indexOf('/');
        if (slash < 0) {
            hostPort = hostPart;
            database = "illbethere";
        } else {
            hostPort = hostPart.substring(0, slash);
            database = hostPart.substring(slash + 1);
        }
        if (database.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL is missing the database name");
        }

        String host;
        int port = 5432;
        int portSep = hostPort.lastIndexOf(':');
        if (portSep > 0) {
            host = hostPort.substring(0, portSep);
            String portText = hostPort.substring(portSep + 1);
            if (!portText.isBlank()) {
                port = Integer.parseInt(portText);
            }
        } else {
            host = hostPort;
        }
        if (host.isBlank()) {
            throw new IllegalArgumentException("DATABASE_URL is missing host");
        }
        return new Parsed(host, port, database, user, password, query);
    }

    private static String decode(String value) {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return value;
        }
    }

    private static String stripQuotes(String raw) {
        if (raw.length() >= 2 && raw.startsWith("\"") && raw.endsWith("\"")) {
            return raw.substring(1, raw.length() - 1);
        }
        return raw;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    record Parsed(String host, int port, String database, String user, String password, String query) {
    }
}
