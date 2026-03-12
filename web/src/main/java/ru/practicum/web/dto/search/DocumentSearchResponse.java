//package ru.practicum.dto.search;
//
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Builder;
//import lombok.Data;
//import ru.practicum.dto.document.DocumentResponse;
//
//import java.util.List;
//
//@Data
//@Builder
//@Schema(description = "Результат поиска документов")
//public class DocumentSearchResponse {
//
//    @Schema(description = "Список найденных документов")
//    private List<DocumentResponse> documents;
//
//    @Schema(description = "Количество найденных документов", example = "15")
//    private long totalCount;
//
//    @Schema(description = "Статистика по фильтрам")
//    private SearchStats stats;
//}
