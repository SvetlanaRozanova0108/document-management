package ru.practicum.web.dto.document;

import lombok.Builder;
import lombok.Data;
import ru.practicum.web.model.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DocumentResponse {

    private String documentNumber;
    private String author;
    private String title;
    private DocumentStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<DocumentHistoryDto> history;
}
