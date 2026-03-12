//package ru.practicum.dto.search;
//
//import io.swagger.v3.oas.annotations.media.Schema;
//import lombok.Builder;
//import lombok.Data;
//
//@Data
//@Builder
//@Schema(description = "Информация о пагинации")
//public class PageInfo {
//
//    @Schema(description = "Номер текущей страницы", example = "0")
//    private int pageNumber;
//
//    @Schema(description = "Размер страницы", example = "20")
//    private int pageSize;
//
//    @Schema(description = "Общее количество элементов", example = "150")
//    private long totalElements;
//
//    @Schema(description = "Общее количество страниц", example = "8")
//    private int totalPages;
//
//    @Schema(description = "Первая ли страница", example = "true")
//    private boolean first;
//
//    @Schema(description = "Последняя ли страница", example = "false")
//    private boolean last;
//
//    @Schema(description = "Количество элементов на текущей странице", example = "20")
//    private int numberOfElements;
//
//    @Schema(description = "Пустая ли страница", example = "false")
//    private boolean empty;
//}