package ru.practicum.utility.processes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import ru.practicum.utility.config.GeneratorConfig;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.web.client.RestTemplate;
import ru.practicum.utility.dto.DocumentRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreateDocumentProcess implements CommandLineRunner {

    private final GeneratorConfig config;
    private static final AtomicInteger successCount = new AtomicInteger(0);
    private static final AtomicInteger errorCount = new AtomicInteger(0);
    private static final DateTimeFormatter LOG_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public void run(String... args) throws Exception {
        int N = config.getCount();

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("              ДОКУМЕНТ ГЕНЕРАТОР - ЗАПУСК");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📋 Параметры генерации:");
        log.info("   └─ Документов к созданию (N): {}", N);
        log.info("   └─ URL API: {}", config.getApiUrl());
        log.info("   └─ Размер пачки: {}", config.getBatchSize());
        log.info("   └─ Количество потоков: {}", config.getThreads());
        log.info("   └─ Файл конфигурации: {}", config.getConfigFile());
        log.info("───────────────────────────────────────────────────────────────");

        long startTime = System.currentTimeMillis();
        LocalDateTime startDateTime = LocalDateTime.now();

        try {
            generateDocuments(config);
        } catch (Exception e) {
            log.error("❌ Критическая ошибка при генерации: {}", e.getMessage(), e);
        }

        long duration = System.currentTimeMillis() - startTime;
        LocalDateTime endDateTime = LocalDateTime.now();

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("              ГЕНЕРАЦИЯ ЗАВЕРШЕНА");
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 Итоговая статистика:");
        log.info("   └─ Запрошено документов (N): {}", N);
        log.info("   └─ Успешно создано: {}", successCount.get());
        log.info("   └─ Ошибок: {}", errorCount.get());
        log.info("   └─ Всего обработано: {}", successCount.get() + errorCount.get());
        log.info("⏱️ Время выполнения:");
        log.info("   └─ Начало: {}", startDateTime.format(LOG_FORMATTER));
        log.info("   └─ Окончание: {}", endDateTime.format(LOG_FORMATTER));
        log.info("   └─ Длительность: {} мс ({} сек)", duration, duration / 1000);
        log.info("   └─ Средняя скорость: {:.2f} док/сек",
                (double) N / (duration / 1000.0));
        log.info("═══════════════════════════════════════════════════════════════");
    }

    private void generateDocuments(GeneratorConfig config) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        ExecutorService executor = Executors.newFixedThreadPool(config.getThreads());
        List<DocumentRequest> documents = generateDocumentRequests(config.getCount());

        int totalBatches = (int) Math.ceil((double) documents.size() / config.getBatchSize());
        log.info("📦 Пакетная обработка:");
        log.info("   └─ Всего пачек: {}", totalBatches);
        log.info("   └─ Размер пачки: {}", config.getBatchSize());
        log.info("───────────────────────────────────────────────────────────────");

        for (int i = 0; i < documents.size(); i += config.getBatchSize()) {
            int batchNumber = i / config.getBatchSize() + 1;
            int endIndex = Math.min(i + config.getBatchSize(), documents.size());
            List<DocumentRequest> batch = documents.subList(i, endIndex);

            executor.submit(() -> processBatch(batch, restTemplate, config, batchNumber, totalBatches));

            // Небольшая задержка между отправкой батчей
            Thread.sleep(50);
        }

        executor.shutdown();
        boolean terminated = executor.awaitTermination(30, TimeUnit.MINUTES);
        if (!terminated) {
            log.warn("⚠️ Таймаут ожидания завершения потоков. Принудительное завершение.");
            executor.shutdownNow();
        }
    }

    private List<DocumentRequest> generateDocumentRequests(int count) {
        List<DocumentRequest> requests = new ArrayList<>();
        String[] authors = {"Иванов И.И.", "Петров П.П.", "Сидоров С.С.",
                "Козлов К.К.", "Смирнова А.А.", "Васильев В.В.", "Федорова Ф.Ф."};
        String[] prefixes = {"Договор", "Счет", "Акт", "Заявка", "Отчет",
                "Спецификация", "Накладная", "Смета", "Заказ", "Наряд"};

        log.info("📄 Генерация {} документов...", count);
        for (int i = 1; i <= count; i++) {
            DocumentRequest request = new DocumentRequest();
            request.setAuthor(authors[i % authors.length]);
            request.setTitle(String.format("%s №%d-%d",
                    prefixes[i % prefixes.length],
                    i,
                    System.currentTimeMillis() % 1000));
            request.setInitiator("generator-" + (i % 5));
            requests.add(request);

            if (i % 100 == 0 || i == count) {
                log.debug("   └─ Сгенерировано {} из {} документов", i, count);
            }
        }
        log.info("   └─ Генерация завершена. Всего документов: {}", requests.size());

        return requests;
    }

    private void processBatch(List<DocumentRequest> batch, RestTemplate restTemplate,
                              GeneratorConfig config, int batchNumber, int totalBatches) {
        long batchStartTime = System.currentTimeMillis();
        LocalDateTime batchStartDateTime = LocalDateTime.now();

        log.info("▶️ [ПАЧКА {}/{}] Начало обработки в {}",
                batchNumber, totalBatches, batchStartDateTime.format(LOG_FORMATTER));
        log.info("   └─ Документов в пачке: {}", batch.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        int batchSuccess = 0;
        int batchErrors = 0;

        for (int i = 0; i < batch.size(); i++) {
            DocumentRequest request = batch.get(i);
            long docStartTime = System.currentTimeMillis();

            try {
                HttpEntity<DocumentRequest> entity = new HttpEntity<>(request, headers);
                restTemplate.postForObject(config.getApiUrl(), entity, String.class);
                successCount.incrementAndGet();
                batchSuccess++;

                long docDuration = System.currentTimeMillis() - docStartTime;
                log.debug("      └─ [{}/{}] Документ создан: {} - {} ({} мс)",
                        i + 1, batch.size(), request.getAuthor(), request.getTitle(), docDuration);

            } catch (Exception e) {
                errorCount.incrementAndGet();
                batchErrors++;
                long docDuration = System.currentTimeMillis() - docStartTime;
                log.error("      └─ [{}/{}] Ошибка создания документа: {} - {} ({} мс): {}",
                        i + 1, batch.size(), request.getTitle(), request.getAuthor(),
                        docDuration, e.getMessage());
            }
        }

        long batchDuration = System.currentTimeMillis() - batchStartTime;
        int processed = successCount.get() + errorCount.get();
        int remaining = config.getCount() - processed;
        double progressPercent = (processed * 100.0) / config.getCount();

        log.info("◀️ [ПАЧКА {}/{}] Завершена в {}",
                batchNumber, totalBatches, LocalDateTime.now().format(LOG_FORMATTER));
        log.info("   └─ Результаты пачки:");
        log.info("      ├─ Успешно: {}", batchSuccess);
        log.info("      ├─ Ошибок: {}", batchErrors);
        log.info("      └─ Время выполнения: {} мс", batchDuration);
        log.info("   └─ Общий прогресс:");
        log.info("      ├─ Обработано: {} из {} ({:.1f}%)", processed, config.getCount(), progressPercent);
        log.info("      ├─ Успешно всего: {}", successCount.get());
        log.info("      ├─ Ошибок всего: {}", errorCount.get());
        log.info("      └─ Осталось: {}", remaining);
        log.info("───────────────────────────────────────────────────────────────");
    }
}