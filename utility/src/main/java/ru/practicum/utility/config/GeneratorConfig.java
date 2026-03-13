package ru.practicum.utility.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.generator")
public class GeneratorConfig {

    private String apiUrl = "http://localhost:8080/api/documents";
    private int count = 100;
    private int batchSize = 50;
    private int threads = 5;
    private String configFile = "application.yml";
}
