package ru.practicum.utility.processes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDocumentProcess implements CommandLineRunner {

    @Value("${CreateDocumentProcess.count}")
    private  int count;

    @Value("${CreateDocumentProcess.url}")
    private String url;


    @Override
    public void run(String... args) throws Exception {
        log.info("Create document process started with url {}, count: {}", url, count);
    }
}
