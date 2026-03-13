package ru.practicum.web.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.dto.batchOperation.BatchOperationRequest;
import ru.practicum.web.dto.batchOperation.BatchOperationResult;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.enums.DocumentStatus;
import ru.practicum.web.model.enums.ResultStatus;
import ru.practicum.web.repository.DocumentRepository;
import ru.practicum.web.repository.SearchRepository;
import ru.practicum.web.service.document.DocumentService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SubmitWorker extends BaseWorker {

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final WorkerStatistics statistics;
    private static final DateTimeFormatter LOG_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final SearchRepository searchRepository;

    public SubmitWorker(DocumentRepository documentRepository,
                        DocumentService documentService,
                        WorkerStatistics statistics,
                        org.springframework.core.env.Environment env, SearchRepository searchRepository) {
        super(Integer.parseInt(env.getProperty("app.batch.size", "100")), "SUBMIT");
        this.documentRepository = documentRepository;
        this.documentService = documentService;
        this.statistics = statistics;
        this.searchRepository = searchRepository;
    }

    @Override
    @Transactional(readOnly = true)
    protected WorkerResult doWork() {
        LocalDateTime workStart = LocalDateTime.now();
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("🔍 SUBMIT-WORKER ЗАПУЩЕН в {}", workStart.format(LOG_FORMATTER));
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 Параметры:");
        log.info("   └─ Размер пачки (batchSize): {}", batchSize);
        log.info("───────────────────────────────────────────────────────────────");

        int totalProcessed = 0;
        int totalErrors = 0;
        int page = 0;
        boolean hasMore = true;

        long workStartTime = System.currentTimeMillis();

        // Получаем общее количество DRAFT документов для логирования прогресса
        long totalDraftCount = searchRepository.countByStatus(DocumentStatus.DRAFT);
        log.info("📊 Статистика на момент запуска:");
        log.info("   └─ Всего документов в статусе DRAFT: {}", totalDraftCount);
        log.info("───────────────────────────────────────────────────────────────");

        while (hasMore) {
            long batchStartTime = System.currentTimeMillis();
            LocalDateTime batchStartDateTime = LocalDateTime.now();

            // Получаем пачку документов в статусе DRAFT
            List<Document> draftDocuments = searchRepository.findByStatus(
                    DocumentStatus.DRAFT,

                    PageRequest.of(page, batchSize)
            );

            if (draftDocuments.isEmpty()) {
                log.info("📭 Нет документов в статусе DRAFT для обработки");
                hasMore = false;
                break;
            }

            int batchNumber = page + 1;
            int processedSoFar = page * batchSize + draftDocuments.size();
            long remaining = totalDraftCount - processedSoFar;
            double progressPercent = (processedSoFar * 100.0) / totalDraftCount;

            log.info("▶️ [ПАЧКА #{}] Обработка начата в {}",
                    batchNumber, batchStartDateTime.format(LOG_FORMATTER));
            log.info("   └─ Документов в пачке: {} (первые ID: {}..{})",
                    draftDocuments.size(),
                    draftDocuments.get(0).getId(),
                    draftDocuments.get(draftDocuments.size() - 1).getId());
            log.info("   └─ Прогресс: {}/{} ({:.1f}%)", processedSoFar, totalDraftCount, progressPercent);
            log.info("   └─ Осталось обработать: {}", remaining);

            // Отправляем на согласование
            BatchResult result = processBatch(draftDocuments, batchNumber);
            totalProcessed += result.successCount;
            totalErrors += result.errorCount;

            long batchDuration = System.currentTimeMillis() - batchStartTime;

            log.info("◀️ [ПАЧКА #{}] Обработка завершена в {}",
                    batchNumber, LocalDateTime.now().format(LOG_FORMATTER));
            log.info("   └─ Результаты пачки:");
            log.info("      ├─ Успешно отправлено: {}", result.successCount);
            log.info("      ├─ Ошибок: {}", result.errorCount);
            log.info("      ├─ Процент успеха: {:.1f}%",
                    (result.successCount * 100.0) / draftDocuments.size());
            log.info("      └─ Время выполнения: {} мс", batchDuration);
            log.info("───────────────────────────────────────────────────────────────");

            // Обновляем статистику
            statistics.recordSubmitRun(result.successCount, result.errorCount, batchDuration);

            page++;
            hasMore = draftDocuments.size() == batchSize;
        }

        long workDuration = System.currentTimeMillis() - workStartTime;
        LocalDateTime workEnd = LocalDateTime.now();

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("✅ SUBMIT-WORKER ЗАВЕРШЕН в {}", workEnd.format(LOG_FORMATTER));
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 Итоги выполнения:");
        log.info("   └─ Обработано пачек: {}", page);
        log.info("   └─ Успешно отправлено: {}", totalProcessed);
        log.info("   └─ Ошибок: {}", totalErrors);
        log.info("   └─ Всего обработано: {}", totalProcessed + totalErrors);
        log.info("⏱️ Время выполнения:");
        log.info("   └─ Начало: {}", workStart.format(LOG_FORMATTER));
        log.info("   └─ Окончание: {}", workEnd.format(LOG_FORMATTER));
        log.info("   └─ Длительность: {} мс ({} сек)", workDuration, workDuration / 1000);
        log.info("═══════════════════════════════════════════════════════════════");

        return new WorkerResult(totalProcessed, totalErrors);
    }

    private BatchResult processBatch(List<Document> documents, int batchNumber) {
        List<Long> documentIds = documents.stream()
                .map(Document::getId)
                .collect(Collectors.toList());

        BatchOperationRequest request = new BatchOperationRequest();
        request.setDocumentIds(documentIds);
        request.setInitiator("SUBMIT-WORKER");
        request.setComment("Автоматическая отправка на согласование");

        log.debug("   └─ Отправка запроса на согласование для документов: {}", documentIds);

        try {
            List<BatchOperationResult> results = documentService.submitDocuments(request);

            long successCount = results.stream()
                    .filter(r -> r.getStatus() == ResultStatus.SUCCESS)
                    .count();

            long errorCount = results.size() - successCount;

            if (errorCount > 0) {
                log.warn("   ⚠️ В пачке #{} обнаружены ошибки:", batchNumber);

                // Детальный лог ошибок
                results.stream()
                        .filter(r -> r.getStatus() != ResultStatus.SUCCESS)
                        .forEach(r -> {
                            switch (r.getStatus()) {
                                case NOT_FOUND:
                                    log.warn("      ├─ Документ {} не найден", r.getDocumentId());
                                    break;
                                case CONFLICT:
                                    log.warn("      ├─ Документ {}: {}", r.getDocumentId(), r.getMessage());
                                    break;
                                default:
                                    log.warn("      ├─ Документ {}: {} - {}",
                                            r.getDocumentId(), r.getStatus(), r.getMessage());
                            }
                        });
            }

            // Лог успешных операций (на уровне debug)
            if (successCount > 0) {
                log.debug("   └─ Успешно отправлены документы: {}",
                        results.stream()
                                .filter(r -> r.getStatus() == ResultStatus.SUCCESS)
                                .map(BatchOperationResult::getDocumentId)
                                .map(String::valueOf)
                                .collect(Collectors.joining(", ")));
            }

            return new BatchResult((int) successCount, (int) errorCount);

        } catch (Exception e) {
            log.error("   ❌ Критическая ошибка при обработке пачки #{}: {}", batchNumber, e.getMessage(), e);
            return new BatchResult(0, documents.size());
        }
    }

    @lombok.Value
    private static class BatchResult {
        int successCount;
        int errorCount;
    }
}
