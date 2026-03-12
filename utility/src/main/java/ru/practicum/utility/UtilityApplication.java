package ru.practicum.utility;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class UtilityApplication {

    public static void main(String[] args) {
        SpringApplication.run(UtilityApplication.class, args);
    }

}
