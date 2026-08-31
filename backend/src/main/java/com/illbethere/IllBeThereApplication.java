package com.illbethere;

import com.illbethere.config.AppProperties;
import com.illbethere.config.DatabaseUrlProcessor;
import com.illbethere.config.DotEnv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class IllBeThereApplication {

    public static void main(String[] args) {
        DotEnv.LoadResult env = DotEnv.applyToSystemProperties();
        if (env.file() != null) {
            boolean hasId = env.values().containsKey("GOOGLE_CLIENT_ID")
                    && !env.values().get("GOOGLE_CLIENT_ID").isBlank();
            boolean hasSecret = env.values().containsKey("GOOGLE_CLIENT_SECRET")
                    && !env.values().get("GOOGLE_CLIENT_SECRET").isBlank();
            System.out.println("I'll Be There: loaded " + env.file().toAbsolutePath()
                    + " (GOOGLE_CLIENT_ID=" + hasId + ", GOOGLE_CLIENT_SECRET=" + hasSecret + ")");
        } else {
            System.out.println("I'll Be There: no .env file found from " + System.getProperty("user.dir"));
        }
        DatabaseUrlProcessor.applyToSystemProperties();
        SpringApplication.run(IllBeThereApplication.class, args);
    }
}
