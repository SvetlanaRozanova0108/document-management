//package ru.practicum.dto.search;
//
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Builder;
//import lombok.Data;
//import org.springframework.format.annotation.DateTimeFormat;
//import ru.practicum.model.enums.DocumentStatus;
//
//import java.time.LocalDateTime;
//
//@Data
//@Builder
//@Schema(description = "Критерии поиска документов")
//public class DocumentSearchCriteria {
//
//    @Schema(description = "Статус документа", example = "DRAFT")
//    private DocumentStatus status;
//
//    @Schema(description = "Автор документа", example = "Иванов И.И.")
//    private String author;
//
//    @Schema(description = "Начало периода (по дате создания)", example = "2024-01-01T00:00:00")
//    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//    private LocalDateTime dateFrom;
//
//    @Schema(description = "Конец периода (по дате создания)", example = "2024-12-31T23:59:59")
//    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
//    private LocalDateTime dateTo;
//}
