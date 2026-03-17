package ru.practicum.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.web.dto.batchOperation.BatchOperationRequest;
import ru.practicum.web.dto.batchOperation.BatchOperationResult;
import ru.practicum.web.dto.document.DocumentRequest;
import ru.practicum.web.dto.document.DocumentResponse;
import ru.practicum.web.dto.search.DocumentSearchRequest;
import ru.practicum.web.model.enums.ResultStatus;
import ru.practicum.web.service.document.DocumentServiceImpl;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/documents")
@Tag(name = "Документы", description = "API для работы с документами")
public class DocumentController {

    private final DocumentServiceImpl documentService;

    @PostMapping
    @Operation(summary = "Создать документ", description = "Создаёт документ в статусе DRAFT. Номер генерируется автоматически")
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody DocumentRequest request) {
        log.info("Request to create document");
        DocumentResponse response = documentService.createDocument(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить документы", description = "API должно вернуть один документ вместе с историей")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable Long id,
            @RequestParam(defaultValue = "false") Boolean includeHistory) {
        log.info("Request to get document with id: {}, includeHistory: {}", id, includeHistory);
        DocumentResponse response = documentService.getDocumentById(id, includeHistory);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/batch")
    @Operation(summary = "Получить документы", description = "API должно вернуть список документов по списку id (пакетное получение) с пагинацией и сортировкой")
    public ResponseEntity<Page<DocumentResponse>> getDocumentsByIds(
            @RequestBody List<Long> ids,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt,desc") String[] sort) {

        Sort sorting = Sort.by(
                Sort.Order.by(sort[0].split(",")[0])
                        .with(Sort.Direction.fromString(sort[0].split(",")[1])));

        Pageable pageable = PageRequest.of(page, size, sorting);

        Page<DocumentResponse> responses = documentService.getDocumentsByIds(ids, pageable);

        return ResponseEntity.ok(responses);
    }

    @PostMapping("/submit")
    @Operation(summary = "Отправить документы на согласование", description = "Переводит документы из статуса DRAFT в SUBMITTED")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "409", description = "Conflict"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<List<BatchOperationResult>> submitDocuments(
            @Valid @RequestBody BatchOperationRequest request) {

        log.info("Request to submit documents. IDs: {}, initiator: {}",
                request.getDocumentIds(), request.getInitiator());

        List<BatchOperationResult> results = documentService.submitDocuments(request);

        log.info("Request completed. Processed: {}, Success: {}",
                results.size(),
                results.stream().filter(r -> r.getStatus() == ResultStatus.SUCCESS).count());

        return ResponseEntity.ok(results);
    }

    @PostMapping("/approve")
    @Operation(summary = "Утвердить документы",
            description = "Переводит документы из статуса SUBMITTED в APPROVED с записью в реестр")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "409", description = "Conflict"),
            @ApiResponse(responseCode = "404", description = "Not found"),
            @ApiResponse(responseCode = "400", description = "Bad Request"),
    })
    public ResponseEntity<List<BatchOperationResult>> approveDocuments(
            @Valid @RequestBody BatchOperationRequest request) {

        log.info("Request to approve documents. IDs: {}, initiator: {}",
                request.getDocumentIds(), request.getInitiator());

        List<BatchOperationResult> results = documentService.approveDocuments(request);

        long successCount = results.stream()
                .filter(r -> r.getStatus() == ResultStatus.SUCCESS)
                .count();

        long registrationErrors = results.stream()
                .filter(r -> r.getStatus() == ResultStatus.REGISTRATION_ERROR)
                .count();

        log.info("Approve completed. Total: {}, Success: {}, Registration errors: {}",
                results.size(), successCount, registrationErrors);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/search")
    @Operation(summary = "Поиск документов по фильтру")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "409", description = "Conflict")
    })
    public ResponseEntity<List<DocumentResponse>> searchDocuments(DocumentSearchRequest request) {

        log.info("Request to search documents with filters: status={}, author={}, dateFrom={}, dateTo={}",
                request.getStatus(), request.getAuthor(), request.getDateFrom(), request.getDateTo());

        List<DocumentResponse> documents = documentService.searchDocuments(request);

        log.info("Search completed. Found {} documents", documents.size());

        return ResponseEntity.ok(documents);
    }
}
