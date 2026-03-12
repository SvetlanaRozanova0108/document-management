package ru.practicum.web.service.approvalRegister;

import ru.practicum.web.model.ApprovalRegister;
import ru.practicum.web.model.Document;

public interface ApprovalRegisterService {

    ApprovalRegister createRegisterEntry(Document document, String approvedBy);

    void deleteRegisterEntry(Long documentId);
}
