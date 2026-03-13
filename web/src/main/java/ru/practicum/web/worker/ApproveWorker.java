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
public class ApproveWorker extends BaseWorker {

    private final DocumentRepository documentRepository;
    private final DocumentService documentService;
    private final WorkerStatistics statistics;
    private static final DateTimeFormatter LOG_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private final SearchRepository searchRepository;

    public ApproveWorker(DocumentRepository documentRepository,
                         DocumentService documentService,
                         WorkerStatistics statistics,
                         org.springframework.core.env.Environment env, SearchRepository searchRepository) {
        super(Integer.parseInt(env.getProperty("app.batch.size", "100")), "APPROVE");
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
        log.info("🔍 APPROVE-WORKER ЗАПУЩЕН в {}", workStart.format(LOG_FORMATTER));
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 Параметры:");
        log.info("   └─ Размер пачки (batchSize): {}", batchSize);
        log.info("───────────────────────────────────────────────────────────────");

        int totalProcessed = 0;
        int totalErrors = 0;
        int totalRegistrationErrors = 0;
        int page = 0;
        boolean hasMore = true;

        long workStartTime = System.currentTimeMillis();

        // Получаем общее количество SUBMITTED документов для логирования прогресса
        long totalSubmittedCount = searchRepository.countByStatus(DocumentStatus.SUBMITTED);
        log.info("📊 Статистика на момент запуска:");
        log.info("   └─ Всего документов в статусе SUBMITTED: {}", totalSubmittedCount);
        log.info("───────────────────────────────────────────────────────────────");

        while (hasMore) {
            long batchStartTime = System.currentTimeMillis();
            LocalDateTime batchStartDateTime = LocalDateTime.now();

            // Получаем пачку документов в статусе SUBMITTED
            List<Document> submittedDocuments = searchRepository.findByStatus(
                    DocumentStatus.SUBMITTED,
                    PageRequest.of(page, batchSize)
            );

            if (submittedDocuments.isEmpty()) {
                log.info("📭 Нет документов в статусе SUBMITTED для обработки");
                hasMore = false;
                break;
            }

            int batchNumber = page + 1;
            int processedSoFar = page * batchSize + submittedDocuments.size();
            long remaining = totalSubmittedCount - processedSoFar;
            double progressPercent = (processedSoFar * 100.0) / totalSubmittedCount;

            log.info("▶️ [ПАЧКА #{}] Обработка начата в {}",
                    batchNumber, batchStartDateTime.format(LOG_FORMATTER));
            log.info("   └─ Документов в пачке: {} (первые ID: {}..{})",
                    submittedDocuments.size(),
                    submittedDocuments.get(0).getId(),
                    submittedDocuments.get(submittedDocuments.size() - 1).getId());
            log.info("   └─ Прогресс: {}/{} ({:.1f}%)", processedSoFar, totalSubmittedCount, progressPercent);
            log.info("   └─ Осталось утвердить: {}", remaining);

            // Утверждаем документы
            BatchResult result = processBatch(submittedDocuments, batchNumber);
            totalProcessed += result.successCount;
            totalErrors += result.errorCount;
            totalRegistrationErrors += result.registrationErrors;

            long batchDuration = System.currentTimeMillis() - batchStartTime;

            log.info("◀️ [ПАЧКА #{}] Обработка завершена в {}",
                    batchNumber, LocalDateTime.now().format(LOG_FORMATTER));
            log.info("   └─ Результаты пачки:");
            log.info("      ├─ Успешно утверждено: {}", result.successCount);
            log.info("      ├─ Ошибок статуса: {}", result.errorCount - result.registrationErrors);
            log.info("      ├─ Ошибок регистрации: {}", result.registrationErrors);
            log.info("      ├─ Процент успеха: {:.1f}%",
                    (result.successCount * 100.0) / submittedDocuments.size());

            if (result.successCount > 0) {
                log.info("      └─ Номера регистрации: {}", result.registrationNumbers);
            } else {
                log.info("      └─ Время выполнения: {} мс", batchDuration);
            }
            log.info("───────────────────────────────────────────────────────────────");

            // Обновляем статистику
            statistics.recordApproveRun(result.successCount, result.errorCount,
                    result.registrationErrors, batchDuration);

            page++;
            hasMore = submittedDocuments.size() == batchSize;
        }

        long workDuration = System.currentTimeMillis() - workStartTime;
        LocalDateTime workEnd = LocalDateTime.now();

        log.info("═══════════════════════════════════════════════════════════════");
        log.info("✅ APPROVE-WORKER ЗАВЕРШЕН в {}", workEnd.format(LOG_FORMATTER));
        log.info("═══════════════════════════════════════════════════════════════");
        log.info("📊 Итоги выполнения:");
        log.info("   └─ Обработано пачек: {}", page);
        log.info("   └─ Успешно утверждено: {}", totalProcessed);
        log.info("   └─ Ошибок статуса: {}", totalErrors - totalRegistrationErrors);
        log.info("   └─ Ошибок регистрации: {}", totalRegistrationErrors);
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
        request.setInitiator("APPROVE-WORKER");
        request.setComment("Автоматическое утверждение");

        log.debug("   └─ Отправка запроса на утверждение для документов: {}", documentIds);

        try {
            List<BatchOperationResult> results = documentService.approveDocuments(request);

            long successCount = results.stream()
                    .filter(r -> r.getStatus() == ResultStatus.SUCCESS)
                    .count();

            long registrationErrors = results.stream()
                    .filter(r -> r.getStatus() == ResultStatus.REGISTRATION_ERROR)
                    .count();

            long conflicts = results.stream()
                    .filter(r -> r.getStatus() == ResultStatus.CONFLICT)
                    .count();

            long notFound = results.stream()
                    .filter(r -> r.getStatus() == ResultStatus.NOT_FOUND)
                    .count();

            long totalErrors = registrationErrors + conflicts + notFound;

            // Собираем номера регистрации для успешных утверждений
            String registrationNumbers = results.stream()
                    .filter(r -> r.getStatus() == ResultStatus.SUCCESS)
                    .map(BatchOperationResult::getRegistrationNumber)
                    .collect(Collectors.joining(", "));

            if (totalErrors > 0) {
                log.warn("   ⚠️ В пачке #{} обнаружены ошибки:", batchNumber);
                log.warn("      ├─ Успешно: {}", successCount);
                log.warn("      ├─ Конфликтов статуса: {}", conflicts);
                log.warn("      ├─ Ошибок регистрации: {}", registrationErrors);
                log.warn("      └─ Не найдено: {}", notFound);

                // Детальный лог ошибок
                results.stream()
                        .filter(r -> r.getStatus() != ResultStatus.SUCCESS)
                        .forEach(r -> {
                            switch (r.getStatus()) {
                                case NOT_FOUND:
                                    log.warn("         ├─ Документ {} не найден", r.getDocumentId());
                                    break;
                                case CONFLICT:
                                    log.warn("         ├─ Документ {}: {}", r.getDocumentId(), r.getMessage());
                                    break;
                                case REGISTRATION_ERROR:
                                    log.warn("         ├─ Документ {}: {}", r.getDocumentId(), r.getMessage());
                                    break;
                            }
                        });
            }

            if (successCount > 0) {
                log.info("   ✅ Успешно утверждены документы с номерами регистрации:");
                log.info("      └─ {}", registrationNumbers);
            }

            return new BatchResult((int) successCount, (int) totalErrors,
                    (int) registrationErrors, registrationNumbers);

        } catch (Exception e) {
            log.error("   ❌ Критическая ошибка при обработке пачки #{}: {}", batchNumber, e.getMessage(), e);
            return new BatchResult(0, documents.size(), 0, "");
        }
    }

    @lombok.Value
    private static class BatchResult {
        int successCount;
        int errorCount;
        int registrationErrors;
        String registrationNumbers;
    }
}
