package ru.practicum.web.worker;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicLong;

@Data
@Component
public class WorkerStatistics {

    // Submit worker stats
    private final AtomicLong submitTotalProcessed = new AtomicLong(0);
    private final AtomicLong submitTotalBatches = new AtomicLong(0);
    private final AtomicLong submitErrors = new AtomicLong(0);

    // Approve worker stats
    private final AtomicLong approveTotalProcessed = new AtomicLong(0);
    private final AtomicLong approveTotalBatches = new AtomicLong(0);
    private final AtomicLong approveErrors = new AtomicLong(0);
    private final AtomicLong approveRegistrationErrors = new AtomicLong(0);

    // Last run info
    private LocalDateTime lastSubmitRun;
    private LocalDateTime lastApproveRun;
    private long lastSubmitDuration;
    private long lastApproveDuration;
    private int lastSubmitProcessed;
    private int lastApproveProcessed;

    public void recordSubmitRun(int processed, int errors, long duration) {
        submitTotalProcessed.addAndGet(processed);
        submitErrors.addAndGet(errors);
        submitTotalBatches.incrementAndGet();
        lastSubmitRun = LocalDateTime.now();
        lastSubmitDuration = duration;
        lastSubmitProcessed = processed;
    }

    public void recordApproveRun(int processed, int errors, int registrationErrors, long duration) {
        approveTotalProcessed.addAndGet(processed);
        approveErrors.addAndGet(errors);
        approveRegistrationErrors.addAndGet(registrationErrors);
        approveTotalBatches.incrementAndGet();
        lastApproveRun = LocalDateTime.now();
        lastApproveDuration = duration;
        lastApproveProcessed = processed;
    }

    public String getSubmitStats() {
        return String.format("SUBMIT: processed=%d, batches=%d, errors=%d, lastRun=%s, lastProcessed=%d",
                submitTotalProcessed.get(), submitTotalBatches.get(), submitErrors.get(),
                lastSubmitRun, lastSubmitProcessed);
    }

    public String getApproveStats() {
        return String.format("APPROVE: processed=%d, batches=%d, errors=%d, regErrors=%d, lastRun=%s, lastProcessed=%d",
                approveTotalProcessed.get(), approveTotalBatches.get(), approveErrors.get(),
                approveRegistrationErrors.get(), lastApproveRun, lastApproveProcessed);
    }
}
