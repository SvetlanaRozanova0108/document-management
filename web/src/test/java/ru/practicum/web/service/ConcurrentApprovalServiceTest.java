package ru.practicum.web.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.web.dto.batchOperation.BatchOperationRequest;
import ru.practicum.web.dto.batchOperation.BatchOperationResult;
import ru.practicum.web.dto.concurrent.ConcurrentRequest;
import ru.practicum.web.dto.concurrent.ConcurrentResult;
import ru.practicum.web.exception.DocumentNotFoundException;
import ru.practicum.web.model.ApprovalRegister;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.enums.DocumentStatus;
import ru.practicum.web.model.enums.ResultStatus;
import ru.practicum.web.repository.ApprovalRegisterRepository;
import ru.practicum.web.repository.DocumentRepository;
import ru.practicum.web.service.concurrent.ConcurrentApprovalServiceImpl;
import ru.practicum.web.service.document.DocumentService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты ConcurrentApprovalService")
class ConcurrentApprovalServiceTest {

    @Mock
    private DocumentService documentService;
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private ApprovalRegisterRepository registerRepository;
    @InjectMocks
    private ConcurrentApprovalServiceImpl testService;

    private Document document;
    private ApprovalRegister register;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        document = new Document();
        document.setId(1L);
        document.setDocumentNumber("DOC-TEST-001");
        document.setAuthor("Тестовый автор");
        document.setTitle("Тестовый документ");
        document.setStatus(DocumentStatus.SUBMITTED);
        document.setCreatedAt(now.minusDays(1));
        document.setUpdatedAt(now.minusDays(1));

        register = ApprovalRegister.builder()
                .id(1L)
                .document(document)
                .approvedBy("test-user")
                .registrationNumber("REG-TEST-123")
                .build();
    }

    @Nested
    @DisplayName("Тесты runConcurrentApproval")
    class RunConcurrentTestTests {

        @Test
        @DisplayName("Должен выбросить исключение при отсутствии документа")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            ConcurrentRequest request = new ConcurrentRequest();
            request.setDocumentId(999L);

            when(documentRepository.findById(999L)).thenReturn(Optional.empty());

            // When/Then
            assertThrows(DocumentNotFoundException.class,
                    () -> testService.runConcurrentApproval(request));
        }

        @Test
        @DisplayName("Должен выполнить тест с одним потоком и одной попыткой")
        void shouldRunTestWithSingleThreadAndAttempt() {
            // Given
            ConcurrentRequest request = new ConcurrentRequest();
            request.setDocumentId(1L);
            request.setThreads(1);
            request.setAttempts(1);
            request.setInitiator("test-user");

            BatchOperationResult successResult = BatchOperationResult.builder()
                    .documentId(1L)
                    .status(ResultStatus.SUCCESS)
                    .message("Successful")
                    .build();

            when(documentRepository.findById(1L)).thenReturn(Optional.of(document));
            when(documentService.approveDocuments(any(BatchOperationRequest.class)))
                    .thenReturn(List.of(successResult));

            // When
            ConcurrentResult result = testService.runConcurrentApproval(request);

            // Then
            assertAll("Проверка результата теста",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getDocumentId()).isEqualTo(1L),
                    () -> assertThat(result.getSuccessfulAttempts()).isEqualTo(1),
                    () -> assertThat(result.getConflictAttempts()).isEqualTo(0),
                    () -> assertThat(result.getTotalAttempts()).isEqualTo(1),
                    () -> assertThat(result.getFinalStatus()).isEqualTo(DocumentStatus.SUBMITTED)
            );
        }

        @Test
        @DisplayName("Должен выполнить тест с несколькими потоками")
        void shouldRunTestWithMultipleThreads() {
            // Given
            ConcurrentRequest request = new ConcurrentRequest();
            request.setDocumentId(1L);
            request.setThreads(5);
            request.setAttempts(2);
            request.setInitiator("test-user");

            BatchOperationResult successResult = BatchOperationResult.builder()
                    .documentId(1L)
                    .status(ResultStatus.SUCCESS)
                    .message("Successful")
                    .build();

            BatchOperationResult conflictResult = BatchOperationResult.builder()
                    .documentId(1L)
                    .status(ResultStatus.CONFLICT)
                    .message("Conflict")
                    .build();

            when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

            // Симулируем одно успешное утверждение, остальные конфликты
            when(documentService.approveDocuments(any(BatchOperationRequest.class)))
                    .thenReturn(List.of(successResult))
                    .thenReturn(List.of(conflictResult))
                    .thenReturn(List.of(conflictResult))
                    .thenReturn(List.of(conflictResult))
                    .thenReturn(List.of(conflictResult))
                    .thenReturn(List.of(conflictResult))
                    .thenReturn(List.of(conflictResult))
                    .thenReturn(List.of(conflictResult))
                    .thenReturn(List.of(conflictResult))
                    .thenReturn(List.of(conflictResult));

            // When
            ConcurrentResult result = testService.runConcurrentApproval(request);

            // Then
            assertThat(result.getSuccessfulAttempts()).isEqualTo(1);
            assertThat(result.getConflictAttempts()).isEqualTo(9);
            assertThat(result.getTotalAttempts()).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Тесты resetDocumentForTest")
    class ResetDocumentForTestTests {

        @Test
        @DisplayName("Должен сбросить APPROVED документ в SUBMITTED")
        void shouldResetApprovedDocument() {
            // Given
            document.setStatus(DocumentStatus.APPROVED);

            // When
            testService.resetDocumentForTest(document);

            // Then
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
            verify(registerRepository).deleteByDocumentId(1L);
            verify(documentRepository).save(document);
        }

        @Test
        @DisplayName("Должен перевести DRAFT документ в SUBMITTED")
        void shouldSetDraftToSubmitted() {
            // Given
            document.setStatus(DocumentStatus.DRAFT);

            // When
            testService.resetDocumentForTest(document);

            // Then
            assertThat(document.getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
            verify(documentRepository).save(document);
        }
    }

    @Nested
    @DisplayName("Тесты верификации реестра")
    class RegisterVerificationTests {

        @Test
        @DisplayName("Должен подтвердить ровно одну запись в реестре")
        void shouldVerifySingleRegisterEntry() {
            // Given
            when(registerRepository.findByDocumentId(1L)).thenReturn(Optional.of(register));

            // When
            boolean result = testService.verifySingleRegisterEntry(1L);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Должен вернуть false при отсутствии записей")
        void shouldReturnFalseWhenNoRegisterEntry() {
            // Given
            when(registerRepository.findByDocumentId(1L)).thenReturn(Optional.empty());

            // When
            boolean result = testService.verifySingleRegisterEntry(1L);

            // Then
            assertThat(result).isFalse();
        }
    }
}
