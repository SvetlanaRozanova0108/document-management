package ru.practicum.web.mapper;

import org.mapstruct.*;
import ru.practicum.web.dto.document.DocumentHistoryDto;
import ru.practicum.web.model.DocumentHistory;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentHistoryMapper {

    // DocumentHistory -> DocumentHistoryDto
    DocumentHistoryDto mapToDocumentHistoryDto(DocumentHistory history);

    // DocumentHistoryList -> DocumentHistoryListDto
    List<DocumentHistoryDto> mapToHistoryDtoList(List<DocumentHistory> histories);

    // DocumentHistoryDto -> DocumentHistory
    DocumentHistory mapToDocumentHistory(DocumentHistoryDto dto);
}
