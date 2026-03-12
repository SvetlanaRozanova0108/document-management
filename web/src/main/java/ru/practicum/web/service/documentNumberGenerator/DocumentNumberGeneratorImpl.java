package ru.practicum.web.service.documentNumberGenerator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentNumberGeneratorImpl implements DocumentNumberGenerator{

    private final AtomicLong counter = new AtomicLong(0);
    private String lastDate = "";

    // Генерация уникального номера документа в формате: DOC-ГГГГММДД-XXXXXX
    public String generateNumber() {
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

        // Если дата изменилась, сбрасываем счетчик
        if (!currentDate.equals(lastDate)) {
            synchronized (this) {
                if (!currentDate.equals(lastDate)) {
                    counter.set(0);
                    lastDate = currentDate;
                    log.debug("Date changed to {}, counter reset", currentDate);
                }
            }
        }

        long sequence = counter.incrementAndGet();
        String documentNumber = String.format("DOC-%s-%06d", currentDate, sequence);

        log.debug("Generated document number: {}", documentNumber);
        return documentNumber;
    }
}
