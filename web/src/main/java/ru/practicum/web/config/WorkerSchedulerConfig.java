package ru.practicum.web.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import ru.practicum.web.worker.ApproveWorker;
import ru.practicum.web.worker.SubmitWorker;
import ru.practicum.web.worker.WorkerStatistics;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class WorkerSchedulerConfig {

    private final SubmitWorker submitWorker;
    private final ApproveWorker approveWorker;
    private final WorkerProperties properties;
    private final WorkerStatistics statistics;

    @Scheduled(fixedDelayString = "${app.worker.submit.fixed-delay:60000}",
            initialDelayString = "${app.worker.submit.initial-delay:10000}")
    public void runSubmitWorker() {
        if (properties.getWorker().getSubmit().isEnabled()) {
            log.info("Scheduled trigger for SubmitWorker");
            submitWorker.execute();
            log.debug("Current statistics: {}", statistics.getSubmitStats());
        } else {
            log.debug("SubmitWorker is disabled");
        }
    }

    @Scheduled(fixedDelayString = "${app.worker.approve.fixed-delay:60000}",
            initialDelayString = "${app.worker.approve.initial-delay:30000}")
    public void runApproveWorker() {
        if (properties.getWorker().getApprove().isEnabled()) {
            log.info("Scheduled trigger for ApproveWorker");
            approveWorker.execute();
            log.debug("Current statistics: {}", statistics.getApproveStats());
        } else {
            log.debug("ApproveWorker is disabled");
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Каждый день в 2 часа ночи
    public void logDailyStatistics() {
        log.info("========== DAILY WORKER STATISTICS ==========");
        log.info(statistics.getSubmitStats());
        log.info(statistics.getApproveStats());
        log.info("==============================================");
    }
}
