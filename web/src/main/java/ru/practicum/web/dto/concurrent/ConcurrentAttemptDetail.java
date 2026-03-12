package ru.practicum.web.dto.concurrent;

import lombok.Builder;
import lombok.Data;
import ru.practicum.web.model.enums.ResultStatus;

import java.time.LocalDateTime;

@Data
@Builder
public class ConcurrentAttemptDetail {
    private int threadId;
    private int attemptNumber;
    private String threadName;
    private ResultStatus result;
    private String message;
    private LocalDateTime attemptTime;
    private long durationMs;
}
