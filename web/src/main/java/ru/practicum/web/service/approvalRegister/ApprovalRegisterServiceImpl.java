package ru.practicum.web.service.approvalRegister;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.model.ApprovalRegister;
import ru.practicum.web.model.Document;
import ru.practicum.web.repository.ApprovalRegisterRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRegisterServiceImpl implements ApprovalRegisterService {

    private final ApprovalRegisterRepository registerRepository;

    // Создать запись в реестре утверждений
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ApprovalRegister createRegisterEntry(Document document, String approvedBy) {
        log.debug("Creating register entry for document {} by {}", document.getId(), approvedBy);

        try {
            // Проверяем, нет ли уже записи для этого документа
            if (registerRepository.existsByDocumentId(document.getId())) {
                log.warn("Register entry already exists for document {}", document.getId());
                throw new RuntimeException("The registry entry already exists for the document " + document.getId());
            }

            // Создаем новую запись
            ApprovalRegister register = ApprovalRegister.builder()
                    .document(document)
                    .approvedBy(approvedBy)
                    .registrationNumber(generateRegistrationNumber())
                    .build();

            ApprovalRegister saved = registerRepository.save(register);
            log.info("Register entry created for document {} with number {}",
                    document.getId(), saved.getRegistrationNumber());

            return saved;

        } catch (Exception e) {
            log.error("Failed to create register entry for document {}: {}",
                    document.getId(), e.getMessage(), e);
            throw new RuntimeException("Error creating an entry in the registry: " + e.getMessage(), e);
        }
    }

    // Удалить запись из реестра (для отката при ошибках)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteRegisterEntry(Long documentId) {
        log.debug("Deleting register entry for document {}", documentId);
        registerRepository.deleteByDocumentId(documentId);
    }

    // Проверить существование записи в реестре
    @Transactional(readOnly = true)
    public boolean checkRegisterEntryExists(Long documentId) {
        return registerRepository.existsByDocumentId(documentId);
    }

    // Сгенерировать уникальный номер регистрации
    private String generateRegistrationNumber() {
        return "REG-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() +
                "-" + System.currentTimeMillis();
    }
}
