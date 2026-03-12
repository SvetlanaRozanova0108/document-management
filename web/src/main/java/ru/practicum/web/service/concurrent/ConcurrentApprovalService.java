package ru.practicum.web.service.concurrent;

import ru.practicum.web.dto.concurrent.ConcurrentRequest;
import ru.practicum.web.dto.concurrent.ConcurrentResult;
import ru.practicum.web.model.Document;

public interface ConcurrentApprovalService {

    ConcurrentResult runConcurrentApproval(ConcurrentRequest request);
    void resetDocumentForTest(Document document);

    boolean verifySingleRegisterEntry(Long documentId);
}