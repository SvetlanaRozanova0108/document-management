//package ru.practicum.dto.search;
//
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Builder;
//import lombok.Data;
//
//import java.util.Map;
//
//@Data
//@Builder
//@Schema(description = "Статистика поиска")
//public class SearchStats {
//
//    @Schema(description = "Количество документов по статусам")
//    private Map<String, Long> countByStatus;
//
//    @Schema(description = "Количество уникальных авторов")
//    private long uniqueAuthorsCount;
//
//    @Schema(description = "Самый старый документ (дата)")
//    private String oldestDocument;
//
//    @Schema(description = "Самый новый документ (дата)")
//    private String newestDocument;
//}
