package ru.practicum.web.dto.concurrent;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Запрос на тестирование конкурентного утверждения")
public class ConcurrentRequest {

    @NotNull(message = "ID документа обязателен")
    @Min(value = 1, message = "ID документа должен быть положительным")
    @Schema(description = "ID документа для тестирования", example = "1")
    private Long documentId;

    @Min(value = 1, message = "Количество потоков должно быть не менее 1")
    @Max(value = 50, message = "Количество потоков не может превышать 50")
    @Schema(description = "Количество параллельных потоков", example = "10")
    private int threads = 10;

    @Min(value = 1, message = "Количество попыток должно быть не менее 1")
    @Max(value = 100, message = "Количество попыток не может превышать 100")
    @Schema(description = "Количество попыток в каждом потоке", example = "5")
    private int attempts = 5;

    @Schema(description = "Инициатор тестирования", example = "test-user")
    private String initiator = "test-user";
}
