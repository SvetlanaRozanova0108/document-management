package ru.practicum.web.service.document;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.practicum.web.dto.batchOperation.BatchOperationRequest;
import ru.practicum.web.dto.batchOperation.BatchOperationResult;
import ru.practicum.web.dto.document.DocumentRequest;
import ru.practicum.web.dto.document.DocumentResponse;
import ru.practicum.web.dto.search.DocumentSearchRequest;

import java.util.List;

public interface DocumentService {

    DocumentResponse createDocument(DocumentRequest request);

    DocumentResponse getDocumentById(Long id, Boolean includeHistory);

    Page<DocumentResponse> getDocumentsByIds(List<Long> ids, Pageable pageable);

    List<BatchOperationResult> submitDocuments(BatchOperationRequest request);

    List<BatchOperationResult> approveDocuments(BatchOperationRequest request);

    List<DocumentResponse> searchDocuments(DocumentSearchRequest request);
}



