package ru.practicum.web.worker;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
public abstract class BaseWorker {

    protected final int batchSize;
    protected final String workerName;
    protected LocalDateTime lastRunTime;
    protected int totalProcessed = 0;
    protected int totalErrors = 0;

    protected static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    protected BaseWorker(int batchSize, String workerName) {
        this.batchSize = batchSize;
        this.workerName = workerName;
    }

    @Async
    public void execute() {
        LocalDateTime startTime = LocalDateTime.now();
        log.info("┌─────────────────────────────────────────────────");
        log.info("│ {} Worker started at {}", workerName, startTime.format(TIME_FORMATTER));
        log.info("│ Batch size: {}", batchSize);
        log.info("├─────────────────────────────────────────────────");

        long startNanos = System.nanoTime();
        int processedInThisRun = 0;
        int errorsInThisRun = 0;

        try {
            WorkerResult result = doWork();
            processedInThisRun = result.getProcessed();
            errorsInThisRun = result.getErrors();

            totalProcessed += processedInThisRun;
            totalErrors += errorsInThisRun;
            lastRunTime = LocalDateTime.now();

        } catch (Exception e) {
            log.error("│ CRITICAL ERROR in {} worker: {}", workerName, e.getMessage(), e);
            errorsInThisRun++;
            totalErrors++;
        }

        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;

        log.info("├─────────────────────────────────────────────────");
        log.info("│ {} Worker completed", workerName);
        log.info("│ Processed in this run: {}", processedInThisRun);
        log.info("│ Errors in this run: {}", errorsInThisRun);
        log.info("│ Duration: {} ms", durationMs);
        log.info("│ Total processed overall: {}", totalProcessed);
        log.info("│ Total errors overall: {}", totalErrors);
        log.info("│ Last run time: {}", lastRunTime.format(TIME_FORMATTER));
        log.info("└─────────────────────────────────────────────────");
    }

    protected abstract WorkerResult doWork();

    @lombok.Value
    protected static class WorkerResult {
        int processed;
        int errors;
    }
}
