package com.illbethere;

import com.illbethere.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class IllBeThereApplication {

    public static void main(String[] args) {
        SpringApplication.run(IllBeThereApplication.class, args);
    }
}
