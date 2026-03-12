package ru.practicum.web.dto.document;

import lombok.Builder;
import lombok.Data;
import ru.practicum.web.model.DocumentHistory;

import java.time.LocalDateTime;

@Data
@Builder
public class DocumentHistoryDto {

    private String initiator;
    private DocumentHistory.DocumentAction action;
    private String comment;
    private LocalDateTime createdAt;
}

