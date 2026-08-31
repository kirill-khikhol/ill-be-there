package com.illbethere.config;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * Render and similar PaaS expose DATABASE_URL as postgres://user:pass@host/db.
 * Spring Boot expects a JDBC URL plus separate username/password.
 */
public class DatabaseUrlProcessor implements EnvironmentPostProcessor, Ordered {

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = firstNonBlank(
                env(environment, "DATABASE_URL"),
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("database.url"));
        if (raw != null) {
            raw = stripQuotes(raw.replace("\r", "").replace("\n", "").trim());
        }

        boolean onRender = firstNonBlank(env(environment, "RENDER"), environment.getProperty("RENDER")) != null;
        System.out.println("I'll Be There: DATABASE_URL present=" + (raw != null && !raw.isBlank())
                + (raw == null || raw.isBlank()
                ? ""
                : " length=" + raw.length() + " starts=" + raw.substring(0, Math.min(18, raw.length()))));

        if (raw == null || raw.isBlank()) {
            Parsed fromParts = fromDiscreteParts(environment);
            if (fromParts != null) {
                apply(environment, fromParts);
                return;
            }
            if (onRender) {
                throw new IllegalStateException(
                        "DATABASE_URL is not set on this web service. "
                                + "Environment → Add from Database → Connection String, key DATABASE_URL.");
            }
            return;
        }

        if (raw.startsWith("jdbc:")) {
            applyJdbc(environment, raw);
            return;
        }

        if (!raw.startsWith("postgres://") && !raw.startsWith("postgresql://")) {
            throw new IllegalStateException(
                    "DATABASE_URL must be a connection string starting with postgresql:// "
                            + "(got starts=" + raw.substring(0, Math.min(24, raw.length()))
                            + "). Use Add from Database → Connection String, not Host.");
        }

        apply(environment, parse(raw));
    }

    private static Parsed fromDiscreteParts(ConfigurableEnvironment environment) {
        String host = firstNonBlank(env(environment, "PGHOST"), env(environment, "SPRING_DATASOURCE_HOST"));
        String user = firstNonBlank(env(environment, "PGUSER"), env(environment, "SPRING_DATASOURCE_USERNAME"));
        String password = firstNonBlank(env(environment, "PGPASSWORD"), env(environment, "SPRING_DATASOURCE_PASSWORD"));
        String database = firstNonBlank(env(environment, "PGDATABASE"), env(environment, "SPRING_DATASOURCE_NAME"));
        String portText = firstNonBlank(env(environment, "PGPORT"), "5432");
        if (host == null || user == null || password == null || database == null) {
            return null;
        }
        int port = Integer.parseInt(portText);
        return new Parsed(host, port, database, user, password, null);
    }

    private static void applyJdbc(ConfigurableEnvironment environment, String jdbc) {
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", jdbc);
        String user = firstNonBlank(env(environment, "PGUSER"), env(environment, "SPRING_DATASOURCE_USERNAME"));
        String password = firstNonBlank(env(environment, "PGPASSWORD"), env(environment, "SPRING_DATASOURCE_PASSWORD"));
        if (user != null) {
            props.put("spring.datasource.username", user);
        }
        if (password != null) {
            props.put("spring.datasource.password", password);
        }
        environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
        System.out.println("I'll Be There: datasource using jdbc URL as provided");
    }

    private static void apply(ConfigurableEnvironment environment, Parsed parsed) {
        String jdbc = "jdbc:postgresql://" + parsed.host + ":" + parsed.port + "/" + parsed.database;
        if (parsed.query != null && !parsed.query.isBlank()) {
            jdbc += (jdbc.contains("?") ? "&" : "?") + parsed.query;
        } else if (parsed.host.contains("render.com") && !jdbc.contains("sslmode")) {
            jdbc += "?sslmode=require";
        }
        Map<String, Object> props = new HashMap<>();
        props.put("spring.datasource.url", jdbc);
        props.put("spring.datasource.username", parsed.user);
        props.put("spring.datasource.password", parsed.password);
        environment.getPropertySources().addFirst(new MapPropertySource("databaseUrl", props));
        System.out.println("I'll Be There: datasource host=" + parsed.host
                + " port=" + parsed.port + " db=" + parsed.database + " user=" + parsed.user);
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

    private static String env(ConfigurableEnvironment environment, String key) {
        Object value = environment.getSystemEnvironment().get(key);
        return value == null ? null : value.toString();
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
