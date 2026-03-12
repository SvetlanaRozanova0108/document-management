package ru.practicum.web.dto.concurrent;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import ru.practicum.web.model.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@Schema(description = "Результат тестирования конкурентного утверждения")
public class ConcurrentResult {

    @Schema(description = "ID документа", example = "1")
    private Long documentId;

    @Schema(description = "Номер документа", example = "DOC-20240227-000001")
    private String documentNumber;

    @Schema(description = "Финальный статус документа", example = "APPROVED")
    private DocumentStatus finalStatus;

    @Schema(description = "Количество успешных попыток", example = "1")
    private int successfulAttempts;

    @Schema(description = "Количество конфликтов", example = "49")
    private int conflictAttempts;

    @Schema(description = "Количество ошибок", example = "0")
    private int errorAttempts;

    @Schema(description = "Всего попыток", example = "50")
    private int totalAttempts;

    @Schema(description = "Детализация по типам результатов")
    private Map<String, Integer> resultDetails;

    @Schema(description = "Номер регистрации в реестре", example = "REG-TEST-123456")
    private String registrationNumber;

    @Schema(description = "Время выполнения теста (мс)", example = "1250")
    private long executionTimeMs;

    @Schema(description = "Время начала теста")
    private LocalDateTime testStartTime;

    @Schema(description = "Время окончания теста")
    private LocalDateTime testEndTime;

    @Schema(description = "Успешен ли тест (ровно одно утверждение)", example = "true")
    private boolean testPassed;

    @Schema(description = "Сообщение о результате", example = "Тест пройден: ровно одно утверждение")
    private String message;
}
