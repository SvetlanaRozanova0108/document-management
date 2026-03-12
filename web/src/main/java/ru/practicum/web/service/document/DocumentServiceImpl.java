package ru.practicum.web.service.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.web.dto.search.DocumentSearchRequest;
import ru.practicum.web.model.ApprovalRegister;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.DocumentHistory;
import ru.practicum.web.dto.batchOperation.BatchOperationRequest;
import ru.practicum.web.dto.batchOperation.BatchOperationResult;
import ru.practicum.web.dto.document.DocumentHistoryDto;
import ru.practicum.web.dto.document.DocumentRequest;
import ru.practicum.web.dto.document.DocumentResponse;
import ru.practicum.web.exception.DocumentNotFoundException;
import ru.practicum.web.mapper.DocumentHistoryMapper;
import ru.practicum.web.mapper.DocumentMapper;
import ru.practicum.web.model.enums.DocumentStatus;
import ru.practicum.web.model.enums.ResultStatus;
import ru.practicum.web.repository.DocumentRepository;
import ru.practicum.web.repository.SearchRepository;
import ru.practicum.web.service.approvalRegister.ApprovalRegisterService;
import ru.practicum.web.service.documentNumberGenerator.DocumentNumberGenerator;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentMapper documentMapper;
    private final DocumentHistoryMapper documentHistoryMapper;
    private final DocumentNumberGenerator numberGenerator;
    private final ApprovalRegisterService registerService;
    private final SearchRepository searchRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    // Создать документ в статусе DRAFT
    @Override
    @Transactional
    public DocumentResponse createDocument(DocumentRequest request) {
        log.debug("Creating new document with title: {}", request.getTitle());
        long startTime = System.currentTimeMillis();

        Document document = new Document();
        document.setAuthor(request.getAuthor());
        document.setTitle(request.getTitle());
        document.setStatus(DocumentStatus.DRAFT);
        document.setDocumentNumber(numberGenerator.generateNumber());

        DocumentHistory history = new DocumentHistory();
        history.setInitiator(request.getInitiator());
        history.setAction(DocumentHistory.DocumentAction.SUBMIT);
        history.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
        history.setComment("Document created");
        document.addHistory(history);

        Document saved = documentRepository.save(document);

        long executionTime = System.currentTimeMillis() - startTime;
        log.debug("Document created successfully. ID: {}, Number: {}, Time: {}ms",
                saved.getId(), saved.getDocumentNumber(), executionTime);

        return documentMapper.mapToDocumentResponse(saved);
    }

    // Вернуть один документ вместе с историей
    @Override
    public DocumentResponse getDocumentById(Long id, Boolean includeHistory) {
        log.debug("Fetching document with ID: {}", id);

        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException("Document not found with id: " + id));

        DocumentResponse response = documentMapper.mapToDocumentResponse(document);

        if (includeHistory && document.getHistory() != null) {
            response.setHistory(document.getHistory().stream()
                    .map(documentHistoryMapper::mapToDocumentHistoryDto)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    // Вернуть список документов по списку id (пакетное получение) с пагинацией
    @Override
    public Page<DocumentResponse> getDocumentsByIds(List<Long> ids, Pageable pageable) {
        log.debug("Fetching documents by IDs: {}, page: {}, size: {}", ids, pageable.getPageNumber(), pageable.getPageSize());

        Page<Document> documentPage = documentRepository.findByIdIn(ids, pageable);

        return documentPage.map(document -> {
            DocumentResponse response = documentMapper.mapToDocumentResponse(document);

            if (document.getHistory() != null && !document.getHistory().isEmpty()) {
                List<DocumentHistoryDto> recentHistory = document.getHistory()
                        .stream()
                        .sorted((h1, h2) -> h2.getCreatedAt().compareTo(h1.getCreatedAt()))
                        .limit(5)
                        .map(documentHistoryMapper::mapToDocumentHistoryDto)
                        .collect(Collectors.toList());

                response.setHistory(recentHistory);
            }

            return response;
        });
    }

    // Отправить документы на согласование
    @Override
    @Transactional
    public List<BatchOperationResult> submitDocuments(BatchOperationRequest request) {
        log.info("Starting batch submit for {} documents", request.getDocumentIds().size());
        long startTime = System.currentTimeMillis();

        List<BatchOperationResult> results = new ArrayList<>();

        for (Long documentId : request.getDocumentIds()) {
            try {
                BatchOperationResult result = submitSingleDocument(
                        documentId,
                        request.getInitiator(),
                        request.getComment()
                );
                results.add(result);

                log.debug("Document {} processed with status: {}", documentId, result.getStatus());

            } catch (Exception e) {
                log.error("Unexpected error processing document ID: {}", documentId, e);
                results.add(BatchOperationResult.builder()
                        .documentId(documentId)
                        .status(ResultStatus.CONFLICT)
                        .message("Internal error: " + e.getMessage())
                        .build());
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        long successCount = results.stream()
                .filter(r -> r.getStatus() == ResultStatus.SUCCESS)
                .count();

        log.info("Batch submit completed. Total: {}, Success: {}, Time: {}ms",
                request.getDocumentIds().size(), successCount, executionTime);

        return results;
    }

    // Утвердить документы (SUBMITTED -> APPROVED)
    @Override
    @Transactional
    public List<BatchOperationResult> approveDocuments(BatchOperationRequest request) {
        log.info("Starting batch approve for {} documents", request.getDocumentIds().size());
        long startTime = System.currentTimeMillis();

        List<BatchOperationResult> results = new ArrayList<>();

        for (Long documentId : request.getDocumentIds()) {
            try {
                BatchOperationResult result = approveSingleDocument(
                        documentId,
                        request.getInitiator(),
                        request.getComment()
                );
                results.add(result);

                log.debug("Document {} processed with status: {}", documentId, result.getStatus());

            } catch (Exception e) {
                log.error("Unexpected error processing document ID: {}", documentId, e);
                results.add(BatchOperationResult.builder()
                        .documentId(documentId)
                        .status(ResultStatus.CONFLICT)
                        .message("Internal error: " + e.getMessage())
                        .build());
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        long successCount = results.stream()
                .filter(r -> r.getStatus() == ResultStatus.SUCCESS)
                .count();

        log.info("Batch approve completed. Total: {}, Success: {}, Time: {}ms",
                request.getDocumentIds().size(), successCount, executionTime);

        return results;
    }

    // Обработать один документ в отдельной транзакции
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected BatchOperationResult submitSingleDocument(Long documentId, String initiator, String comment) {

        // Пытаемся найти документ с блокировкой
        Optional<Document> optionalDoc = documentRepository.findByIdWithLock(documentId);

        if (optionalDoc.isEmpty()) {
            log.warn("Document not found: {}", documentId);
            return BatchOperationResult.builder()
                    .documentId(documentId)
                    .status(ResultStatus.NOT_FOUND)
                    .message("Document with ID " + documentId + " not found")
                    .build();
        }

        Document document = optionalDoc.get();

        // Проверяем возможность перехода статуса
        if (document.getStatus() != DocumentStatus.DRAFT) {
            log.warn("Invalid status transition for document {}: {} -> SUBMITTED",
                    documentId, document.getStatus());

            return BatchOperationResult.builder()
                    .documentId(documentId)
                    .status(ResultStatus.CONFLICT)
                    .message(String.format("It is not possible to send a document with the status for approval %s", document.getStatus()))
                    .build();
        }

        try {
            // Обновляем статус документа
            document.setStatus(DocumentStatus.SUBMITTED);

            // Создаем запись в истории
            DocumentHistory history = new DocumentHistory();
            history.setInitiator(initiator);
            history.setAction(DocumentHistory.DocumentAction.SUBMIT);
            history.setCreatedAt(LocalDateTime.now(ZoneOffset.UTC));
            history.setComment(comment != null ? comment : "Submitted for approval");

            document.addHistory(history);

            // Сохраняем изменения
            documentRepository.save(document);

            log.info("Document {} successfully submitted by {}", documentId, initiator);

            return BatchOperationResult.builder()
                    .documentId(documentId)
                    .status(ResultStatus.SUCCESS)
                    .message("The document has been successfully submitted for approval")
                    .build();

        } catch (Exception e) {
            log.error("Error submitting document {}: {}", documentId, e.getMessage(), e);

            return BatchOperationResult.builder()
                    .documentId(documentId)
                    .status(ResultStatus.CONFLICT)
                    .message("Error when saving: " + e.getMessage())
                    .build();
        }
    }

    // Обработать утверждение одного документа в отдельной транзакции
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected BatchOperationResult approveSingleDocument(Long documentId, String initiator, String comment) {

        // Пытаемся найти документ с блокировкой
        Optional<Document> optionalDoc = documentRepository.findByIdWithLock(documentId);

        if (optionalDoc.isEmpty()) {
            log.warn("Document not found: {}", documentId);
            return BatchOperationResult.builder()
                    .documentId(documentId)
                    .status(ResultStatus.NOT_FOUND)
                    .message("Document with ID " + documentId + " not found")
                    .build();
        }

        Document document = optionalDoc.get();

        // Проверяем возможность перехода статуса
        if (document.getStatus() != DocumentStatus.SUBMITTED) {
            log.warn("Invalid status transition for document {}: {} -> APPROVED",
                    documentId, document.getStatus());

            return BatchOperationResult.builder()
                    .documentId(documentId)
                    .status(ResultStatus.CONFLICT)
                    .message(String.format("The document status cannot be approved %s",
                            document.getStatus()))
                    .build();
        }

        ApprovalRegister registerEntry = null;

        try {
            // Шаг 1: Создаем запись в реестре утверждений
            // Это делается в отдельной транзакции
            registerEntry = registerService.createRegisterEntry(document, initiator);

            // Шаг 2: Обновляем статус документа
            document.setStatus(DocumentStatus.APPROVED);

            // Шаг 3: Создаем запись в истории
            DocumentHistory history = new DocumentHistory();
            history.setDocument(document);
            history.setInitiator(initiator);
            history.setAction(DocumentHistory.DocumentAction.APPROVE);
            history.setComment(comment != null ? comment : "Документ утвержден");

            document.addHistory(history);

            // Сохраняем изменения
            documentRepository.save(document);

            log.info("Document {} successfully approved by {}. Registration number: {}",
                    documentId, initiator, registerEntry.getRegistrationNumber());

            return BatchOperationResult.builder()
                    .documentId(documentId)
                    .status(ResultStatus.SUCCESS)
                    .message("The document has been successfully approved")
                    .registrationNumber(registerEntry.getRegistrationNumber())
                    .build();

        } catch (Exception e) {
            log.error("Error approving document {}: {}", documentId, e.getMessage(), e);

            // Если запись в реестре была создана, но что-то пошло дальше,
            // удаляем её для консистентности
            if (registerEntry != null && registerEntry.getId() != null) {
                try {
                    registerService.deleteRegisterEntry(documentId);
                    log.info("Rolled back register entry for document {}", documentId);
                } catch (Exception ex) {
                    log.error("Failed to rollback register entry for document {}", documentId, ex);
                }
            }

            // Определяем тип ошибки
            if (e.getMessage() != null && e.getMessage().contains("реестр")) {
                return BatchOperationResult.builder()
                        .documentId(documentId)
                        .status(ResultStatus.REGISTRATION_ERROR)
                        .message("Ошибка регистрации в реестре: " + e.getMessage())
                        .build();
            }

            return BatchOperationResult.builder()
                    .documentId(documentId)
                    .status(ResultStatus.CONFLICT)
                    .message("Ошибка при утверждении: " + e.getMessage())
                    .build();
        }
    }

    // Поиск документов по фильтру
    @Override
    public List<DocumentResponse> searchDocuments(DocumentSearchRequest request) {

        log.info("Searching documents with filters: status={}, author={}, dateFrom={}, dateTo={}",
                request.getStatus(), request.getAuthor(), request.getDateFrom(), request.getDateTo());

        long startTime = System.currentTimeMillis();

        List<Document> documents = performSearch(request.getStatus(), request.getAuthor(), request.getDateFrom(), request.getDateTo());

        List<DocumentResponse> responses = documents.stream()
                .map(documentMapper::mapToDocumentResponse)
                .collect(Collectors.toList());

        log.info("Search completed. Found {} documents, time: {}ms",
                responses.size(), System.currentTimeMillis() - startTime);

        return responses;
    }

    // Выполнение поиска в зависимости от переданных параметров
    private List<Document> performSearch(DocumentStatus status, String author,
                                         LocalDateTime dateFrom, LocalDateTime dateTo) {

        // Все параметры присутствуют
        if (status != null && author != null && dateFrom != null && dateTo != null) {
            return searchRepository.findByStatusAndAuthorAndCreatedAtBetween(
                    status, author, dateFrom, dateTo);
        }

        // Статус и автор
        if (status != null && author != null) {
            return searchRepository.findByStatusAndAuthor(status, author);
        }

        // Статус и период
        if (status != null && dateFrom != null && dateTo != null) {
            return searchRepository.findByStatusAndCreatedAtBetween(status, dateFrom, dateTo);
        }

        // Автор и период
        if (author != null && dateFrom != null && dateTo != null) {
            return searchRepository.findByAuthorAndCreatedAtBetween(author, dateFrom, dateTo);
        }

        // Только статус
        if (status != null) {
            return searchRepository.findByStatus(status);
        }

        // Только автор
        if (author != null) {
            return searchRepository.findByAuthorContainingIgnoreCase(author);
        }

        // Только период
        if (dateFrom != null && dateTo != null) {
            return searchRepository.findByCreatedAtBetween(dateFrom, dateTo);
        }

        // Нет фильтров - возвращаем все документы
        return documentRepository.findAll();
    }
}
