package ru.practicum.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app")
public class WorkerProperties {

    private Batch batch = new Batch();
    private Worker worker = new Worker();

    @Data
    public static class Batch {
        private int size = 100;
    }

    @Data
    public static class Worker {
        private SubmitWorker submit = new SubmitWorker();
        private ApproveWorker approve = new ApproveWorker();
    }

    @Data
    public static class SubmitWorker {
        private boolean enabled = true;
        private long fixedDelay = 60000;
        private long initialDelay = 10000;
    }

    @Data
    public static class ApproveWorker {
        private boolean enabled = true;
        private long fixedDelay = 60000;
        private long initialDelay = 30000;
    }
}
