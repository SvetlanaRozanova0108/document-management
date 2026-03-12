package ru.practicum.web.mapper;

import org.mapstruct.*;
import org.mapstruct.factory.Mappers;
import ru.practicum.web.dto.document.DocumentRequest;
import ru.practicum.web.dto.document.DocumentResponse;
import ru.practicum.web.model.Document;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface DocumentMapper {

    DocumentMapper INSTANCE = Mappers.getMapper(DocumentMapper.class);

    // Document -> DocumentResponse
    @Mapping(target = "history", ignore = true)
    DocumentResponse mapToDocumentResponse(Document document);

    // DocumentRequest -> Document
    @Mapping(target = "documentNumber", ignore = true)
    @Mapping(target = "status", constant = "DRAFT")
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "history", ignore = true)
    Document mapToDocument(DocumentRequest request);
}
