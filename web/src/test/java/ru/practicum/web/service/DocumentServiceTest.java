package ru.practicum.web.service;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.practicum.web.dto.batchOperation.BatchOperationRequest;
import ru.practicum.web.dto.batchOperation.BatchOperationResult;
import ru.practicum.web.dto.document.DocumentHistoryDto;
import ru.practicum.web.dto.document.DocumentRequest;
import ru.practicum.web.dto.document.DocumentResponse;
import ru.practicum.web.dto.search.DocumentSearchRequest;
import ru.practicum.web.exception.DocumentNotFoundException;
import ru.practicum.web.mapper.DocumentHistoryMapper;
import ru.practicum.web.mapper.DocumentMapper;
import ru.practicum.web.model.ApprovalRegister;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.DocumentHistory;
import ru.practicum.web.model.enums.DocumentStatus;
import ru.practicum.web.model.enums.ResultStatus;
import ru.practicum.web.repository.DocumentRepository;
import ru.practicum.web.repository.SearchRepository;
import ru.practicum.web.service.approvalRegister.ApprovalRegisterService;
import ru.practicum.web.service.document.DocumentServiceImpl;
import ru.practicum.web.service.documentNumberGenerator.DocumentNumberGenerator;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Тесты DocumentService")
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private SearchRepository searchRepository;
    @Mock
    private DocumentMapper documentMapper;
    @Mock
    private DocumentHistoryMapper documentHistoryMapper;
    @Mock
    private DocumentNumberGenerator numberGenerator;
    @InjectMocks
    private DocumentServiceImpl documentService;
    @Captor
    private ArgumentCaptor<Document> documentCaptor;
    @Mock
    private ApprovalRegisterService registerService;

    private Document document;
    private Document document2;
    private Document document3;
    private Document document4;
    private DocumentResponse documentResponse;
    private DocumentResponse documentResponse2;
    private DocumentResponse documentResponse3;
    private DocumentResponse documentResponse4;
    private DocumentRequest documentRequest;
    private DocumentHistory history1;
    private DocumentHistory history2;
    private DocumentHistoryDto historyDto1;
    private DocumentHistoryDto historyDto2;
    private ApprovalRegister registerEntry;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Создаем тестовый документ
        document = new Document();
        document.setId(1L);
        document.setDocumentNumber("DOC-20240225-000001");
        document.setAuthor("Иванов И.И.");
        document.setTitle("Тестовый документ");
        document.setStatus(DocumentStatus.DRAFT);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        document2 = new Document();
        document2.setId(2L);
        document2.setDocumentNumber("DOC-20240225-000002");
        document2.setAuthor("Андреев А.А.");
        document2.setTitle("Документ 2");
        document2.setStatus(DocumentStatus.SUBMITTED);
        document2.setCreatedAt(now.minusDays(2));
        document2.setUpdatedAt(now.minusDays(2));

        document3 = new Document();
        document3.setId(3L);
        document3.setDocumentNumber("DOC-20240225-000003");
        document3.setAuthor("Борисов Б.Б.");
        document3.setTitle("Документ 3");
        document3.setStatus(DocumentStatus.APPROVED);
        document3.setCreatedAt(now.minusDays(1));
        document3.setUpdatedAt(now.minusDays(1));

        document4 = new Document();
        document4.setId(4L);
        document4.setDocumentNumber("DOC-20240225-000004");
        document4.setAuthor("Сидоров С.С.");
        document4.setTitle("Документ 4");
        document4.setStatus(DocumentStatus.DRAFT);
        document4.setCreatedAt(now);
        document4.setUpdatedAt(now);

        // Создаем историю документа
        history1 = new DocumentHistory();
        history1.setId(1L);
        history1.setDocument(document);
        history1.setInitiator("Андреев А.А.");
        history1.setAction(DocumentHistory.DocumentAction.SUBMIT);
        history1.setComment("Создание документа");
        history1.setCreatedAt(now.minusDays(1));

        history2 = new DocumentHistory();
        history2.setId(2L);
        history2.setDocument(document);
        history2.setInitiator("Петров П.П.");
        history2.setAction(DocumentHistory.DocumentAction.SUBMIT);
        history2.setComment("Отправка на согласование");
        history2.setCreatedAt(now);

        document.setHistory(Arrays.asList(history1, history2));

        // Создаем DTO для ответа
        documentResponse = DocumentResponse.builder()
                .documentNumber("DOC-20240225-000001")
                .author("Иванов И.И.")
                .title("Тестовый документ")
                .status(DocumentStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        documentResponse2 = DocumentResponse.builder()
                .documentNumber("DOC-20240225-000002")
                .author("Андреев А.А.")
                .title("Документ 2")
                .status(DocumentStatus.SUBMITTED)
                .createdAt(now.minusDays(2))
                .updatedAt(now.minusDays(2))
                .build();

        documentResponse3 = DocumentResponse.builder()
                .documentNumber("DOC-20240225-000003")
                .author("Борисов Б.Б.")
                .title("Документ 3")
                .status(DocumentStatus.APPROVED)
                .createdAt(now.minusDays(1))
                .updatedAt(now.minusDays(1))
                .build();

        documentResponse4 = DocumentResponse.builder()
                .documentNumber("DOC-20240225-000004")
                .author("Сидоров С.С.")
                .title("Документ 4")
                .status(DocumentStatus.DRAFT)
                .createdAt(now)
                .updatedAt(now)
                .build();

        // Создаем DTO для истории
        historyDto1 = DocumentHistoryDto.builder()
                .initiator("Иванов И.И.")
                .action(DocumentHistory.DocumentAction.SUBMIT)
                .comment("Создание документа")
                .createdAt(now.minusDays(1))
                .build();

        historyDto2 = DocumentHistoryDto.builder()
                .initiator("Петров П.П.")
                .action(DocumentHistory.DocumentAction.SUBMIT)
                .comment("Отправка на согласование")
                .createdAt(now)
                .build();

        // Создаем запрос на создание документа
        documentRequest = new DocumentRequest();
        documentRequest.setAuthor("Иванов И.И.");
        documentRequest.setTitle("Тестовый документ");
        documentRequest.setInitiator("Иванов И.И.");

        // Запись в реестре для успешного утверждения
        registerEntry = ApprovalRegister.builder()
                .id(100L)
                .document(document2)
                .approvedBy("Иванов И.И.")
                .registrationNumber("REG-20240305-001")
                .approvedAt(now)
                .build();
    }

    @Nested
    @DisplayName("Тесты создания документа")
    class CreateDocumentTests {

        @Test
        @DisplayName("Должен успешно создать документ со статусом DRAFT")
        void shouldCreateDocumentSuccessfully() {
            // Given
            when(documentRepository.save(any(Document.class))).thenReturn(document);

            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);

            // When
            DocumentResponse response = documentService.createDocument(documentRequest);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDocumentNumber()).isEqualTo("DOC-20240225-000001");
            assertThat(response.getAuthor()).isEqualTo(documentRequest.getAuthor());
            assertThat(response.getTitle()).isEqualTo(documentRequest.getTitle());
            assertThat(response.getStatus()).isEqualTo(DocumentStatus.DRAFT);
            assertThat(response.getCreatedAt()).isEqualTo(now);
            assertThat(response.getUpdatedAt()).isEqualTo(now);
            assertThat(response.getHistory()).isNull();
        }

        @Test
        @DisplayName("Должен создать документ с историей о создании")
        void shouldCreateDocumentWithHistory() {
            // Given
            //when(documentMapper.mapToDocument(documentRequest)).thenReturn(document);
            when(numberGenerator.generateNumber()).thenReturn("DOC-20240225-000002");
            //when(documentRepository.save(any(Document.class))).thenReturn(document);
            when(documentRepository.save(any(Document.class)))
                    .thenAnswer(invocation -> {
                        Document doc = invocation.getArgument(0);
                        doc.setId(2L);
                        doc.setCreatedAt(now);
                        doc.setUpdatedAt(now);
                        return doc;
                    });

            when(documentMapper.mapToDocumentResponse(any(Document.class))).thenReturn(documentResponse);

            // When
            documentService.createDocument(documentRequest);

            // Then
            verify(documentRepository).save(documentCaptor.capture());
            Document capturedDocument = documentCaptor.getValue();

            assertThat(capturedDocument).isNotNull();
            assertThat(capturedDocument.getCreatedAt()).isEqualTo(now);
            assertThat(capturedDocument.getUpdatedAt()).isEqualTo(now);
            assertThat(capturedDocument.getHistory()).hasSize(1);

            DocumentHistory history = capturedDocument.getHistory().get(0);
            assertThat(history.getInitiator()).isEqualTo(documentRequest.getInitiator());
            assertThat(history.getAction()).isEqualTo(DocumentHistory.DocumentAction.SUBMIT);
            assertThat(history.getComment()).isEqualTo("Document created");
            assertThat(history.getCreatedAt()).isNotNull();
            assertThat(history.getDocument()).isEqualTo(capturedDocument);

            //verify(documentMapper, times(1)).mapToDocument(documentRequest);
            verify(numberGenerator, times(1)).generateNumber();
            verify(documentRepository, times(1)).save(any(Document.class));
            verify(documentMapper, times(1)).mapToDocumentResponse(any(Document.class));
        }

        @Test
        @DisplayName("Должен выбросить исключение при null запросе")
        void shouldThrowExceptionWhenRequestIsNull() {
            // When/Then
            assertThatThrownBy(() -> documentService.createDocument(null))
                    .isInstanceOf(NullPointerException.class)
                    .hasMessageContaining("request");

            verify(documentMapper, never()).mapToDocument(any());
            verify(numberGenerator, never()).generateNumber();
            verify(documentRepository, never()).save(any());
            verify(documentMapper, never()).mapToDocumentResponse(any());
        }
    }

    @Nested
    @DisplayName("Тесты получения одного документа")
    class GetSingleDocumentTests {

        @Test
        @DisplayName("Должен вернуть документ без истории")
        void shouldReturnDocumentWithoutHistory() {
            // Given
            Long documentId = 1L;
            boolean includeHistory = false;

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);

            // When
            DocumentResponse response = documentService.getDocumentById(documentId, includeHistory);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getDocumentNumber()).isEqualTo("DOC-20240225-000001");
            assertThat(response.getAuthor()).isEqualTo("Иванов И.И.");
            assertThat(response.getTitle()).isEqualTo("Тестовый документ");
            assertThat(response.getStatus()).isEqualTo(DocumentStatus.DRAFT);
            assertThat(response.getCreatedAt()).isEqualTo(now);
            assertThat(response.getUpdatedAt()).isEqualTo(now);
            assertThat(response.getHistory()).isNull();

            verify(documentRepository, times(1)).findById(documentId);
            verify(documentMapper, times(1)).mapToDocumentResponse(document);
            verify(documentHistoryMapper, never()).mapToDocumentHistoryDto(any());
        }

        @Test
        @DisplayName("Должен вернуть документ с историей")
        void shouldReturnDocumentWithHistory() {
            // Given
            Long documentId = 1L;
            boolean includeHistory = true;

            when(documentRepository.findById(documentId)).thenReturn(Optional.of(document));
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);
            when(documentHistoryMapper.mapToDocumentHistoryDto(history1)).thenReturn(historyDto1);
            when(documentHistoryMapper.mapToDocumentHistoryDto(history2)).thenReturn(historyDto2);

            // When
            DocumentResponse response = documentService.getDocumentById(documentId, includeHistory);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getHistory()).isNotNull();
            assertThat(response.getHistory()).hasSize(2);

            {
                DocumentHistoryDto firstHistory = response.getHistory().get(0);
                assertThat(firstHistory.getInitiator()).isEqualTo("Иванов И.И.");
                assertThat(firstHistory.getAction()).isEqualTo(DocumentHistory.DocumentAction.SUBMIT);
                assertThat(firstHistory.getComment()).isEqualTo("Создание документа");
                assertThat(firstHistory.getCreatedAt()).isEqualTo(now.minusDays(1));
            }

            {
                DocumentHistoryDto secondHistory = response.getHistory().get(1);
                assertThat(secondHistory.getInitiator()).isEqualTo("Петров П.П.");
                assertThat(secondHistory.getAction()).isEqualTo(DocumentHistory.DocumentAction.SUBMIT);
                assertThat(secondHistory.getComment()).isEqualTo("Отправка на согласование");
                assertThat(secondHistory.getCreatedAt()).isEqualTo(now);
            }

            verify(documentRepository, times(1)).findById(documentId);
            verify(documentMapper, times(1)).mapToDocumentResponse(document);
            verify(documentHistoryMapper, times(1)).mapToDocumentHistoryDto(history1);
            verify(documentHistoryMapper, times(1)).mapToDocumentHistoryDto(history2);
            verify(documentHistoryMapper, times(2)).mapToDocumentHistoryDto(any(DocumentHistory.class));
        }

        @Test
        @DisplayName("Должен выбросить исключение при отсутствии документа")
        void shouldThrowExceptionWhenDocumentNotFound() {
            // Given
            Long documentId = 999L;
            boolean includeHistory = false;

            when(documentRepository.findById(documentId)).thenReturn(Optional.empty());

            // When/Then
            assertThatThrownBy(() -> documentService.getDocumentById(documentId, includeHistory))
                    .isInstanceOf(DocumentNotFoundException.class)
                    .hasMessageContaining("Document not found with id: " + documentId);

            verify(documentRepository).findById(documentId);
            verify(documentMapper, never()).mapToDocumentResponse(any());
        }
    }

    @Nested
    @DisplayName("Тесты получения документов с пагинацией и сортировкой")
    class GetDocumentsWithPaginationTests {

        @Test
        @DisplayName("Должен вернуть страницу документов по списку ID с пагинацией")
        void shouldReturnPageOfDocumentsByIds() {
            // Given
            List<Long> ids = Arrays.asList(1L, 2L, 3L, 4L, 5L);
            Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

            Document document2 = new Document();
            document2.setId(2L);
            Document document3 = new Document();
            document3.setId(3L);

            List<Document> documents = Arrays.asList(document, document2);
            Page<Document> documentPage = new PageImpl<>(documents, pageable, 5);

            when(documentRepository.findByIdIn(ids, pageable)).thenReturn(documentPage);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);
            when(documentMapper.mapToDocumentResponse(document2)).thenReturn(documentResponse2);

            // When
            Page<DocumentResponse> resultPage = documentService.getDocumentsByIds(ids, pageable);

            // Then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent()).hasSize(2);
            assertThat(resultPage.getTotalElements()).isEqualTo(5);
            assertThat(resultPage.getTotalPages()).isEqualTo(3);
            assertThat(resultPage.getNumber()).isZero();
            assertThat(resultPage.getSize()).isEqualTo(2);

            DocumentResponse firstResponse = resultPage.getContent().get(0);
            assertThat(firstResponse.getCreatedAt()).isEqualTo(now);
            assertThat(firstResponse.getUpdatedAt()).isEqualTo(now);

            DocumentResponse secondResponse = resultPage.getContent().get(1);
            assertThat(secondResponse.getCreatedAt()).isEqualTo(now.minusDays(2));
            assertThat(secondResponse.getUpdatedAt()).isEqualTo(now.minusDays(2));

            verify(documentRepository).findByIdIn(ids, pageable);
            verify(documentMapper, times(1)).mapToDocumentResponse(document);
            verify(documentMapper, times(1)).mapToDocumentResponse(document2);
            verify(documentMapper, times(2)).mapToDocumentResponse(any(Document.class));
        }

        @Test
        @DisplayName("Должен вернуть пустую страницу при отсутствии документов")
        void shouldReturnEmptyPageWhenNoDocumentsFound() {
            // Given
            List<Long> ids = Arrays.asList(999L, 1000L);
            Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

            Page<Document> emptyPage = Page.empty(pageable);

            when(documentRepository.findByIdIn(ids, pageable)).thenReturn(emptyPage);

            // When
            Page<DocumentResponse> resultPage = documentService.getDocumentsByIds(ids, pageable);

            // Then
            assertThat(resultPage).isNotNull();
            assertThat(resultPage.getContent()).isEmpty();
            assertThat(resultPage.getTotalElements()).isZero();

            verify(documentRepository, times(1)).findByIdIn(ids, pageable);
        }

        @Test
        @DisplayName("Должен корректно обрабатывать сортировку")
        void shouldHandleSortingCorrectly() {
            // Given
            List<Long> ids = Arrays.asList(1L, 2L, 3L);
            Pageable pageable = PageRequest.of(0, 2,
                    Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("author")));

            List<Document> sortedDocuments = Arrays.asList(document, document3, document2);
            Page<Document> documentPage = new PageImpl<>(sortedDocuments, pageable, 3);

            when(documentRepository.findByIdIn(ids, pageable)).thenReturn(documentPage);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);
            when(documentMapper.mapToDocumentResponse(document2)).thenReturn(documentResponse2);
            when(documentMapper.mapToDocumentResponse(document3)).thenReturn(documentResponse3);

            // When
            Page<DocumentResponse> resultPage = documentService.getDocumentsByIds(ids, pageable);

            // Then
            assertAll("Проверка результата с сортировкой",
                    () -> assertThat(resultPage).isNotNull(),
                    () -> assertThat(resultPage.getContent()).hasSize(3),
                    () -> assertThat(resultPage.getTotalElements()).isEqualTo(3),
                    () -> assertThat(resultPage.getNumber()).isZero(),
                    () -> assertThat(resultPage.getSize()).isEqualTo(2)
            );

            // Проверяем, что pageable содержит правильную сортировку
            Sort sort = pageable.getSort();
            assertAll("Проверка параметров сортировки",
                    () -> assertThat(sort).isNotNull(),
                    () -> assertThat(sort.getOrderFor("createdAt")).isNotNull(),
                    () -> assertThat(sort.getOrderFor("createdAt").getDirection()).isEqualTo(Sort.Direction.DESC),
                    () -> assertThat(sort.getOrderFor("author")).isNotNull(),
                    () -> assertThat(sort.getOrderFor("author").getDirection()).isEqualTo(Sort.Direction.ASC)
            );

            // Проверяем порядок документов
            List<DocumentResponse> content = resultPage.getContent();
            assertAll("Проверка порядка сортировки",
                    () -> assertThat(content.get(0).getDocumentNumber())
                            .as("Первый документ - самый новый (now)")
                            .isEqualTo("DOC-20240225-000001"),
                    () -> assertThat(content.get(1).getDocumentNumber())
                            .as("Второй документ - средний (now-1)")
                            .isEqualTo("DOC-20240225-000003"),
                    () -> assertThat(content.get(2).getDocumentNumber())
                            .as("Третий документ - самый старый (now-2)")
                            .isEqualTo("DOC-20240225-000002")
            );

            // сортировка по автору
            assertAll("Проверка авторов",
                    () -> assertThat(content.get(0).getAuthor()).isEqualTo("Иванов И.И."),
                    () -> assertThat(content.get(1).getAuthor()).isEqualTo("Борисов Б.Б."),
                    () -> assertThat(content.get(2).getAuthor()).isEqualTo("Андреев А.А.")
            );

            verify(documentRepository, times(1)).findByIdIn(ids, pageable);
            verify(documentMapper, times(3)).mapToDocumentResponse(any(Document.class));
        }
    }

    @Nested
    @DisplayName("Тесты отправки документа")
    class SubmitDocumentTests {

        @Nested
        @DisplayName("Успешные сценарии")
        class SuccessScenariosSubmit {

            @Test
            @DisplayName("Должен успешно отправить один DRAFT документ на согласование")
            void shouldSubmitSingleDraftDocument() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(List.of(1L));
                request.setInitiator("Иванов И.И.");
                request.setComment("Отправка на согласование");

                Document savedDocument = new Document();
                savedDocument.setId(1L);
                savedDocument.setDocumentNumber("DOC-20240225-000001");
                savedDocument.setAuthor("Иванов И.И.");
                savedDocument.setTitle("Тестовый документ");
                savedDocument.setStatus(DocumentStatus.DRAFT); // После сохранения статус SUBMITTED
                savedDocument.setCreatedAt(now);
                savedDocument.setUpdatedAt(now);


                when(documentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(savedDocument));
                when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

                // When
                List<BatchOperationResult> results = documentService.submitDocuments(request);

                // Then
                assertAll("Проверка результата",
                        () -> assertThat(results).hasSize(1),
                        () -> {
                            Assertions.assertNotNull(results);
                            BatchOperationResult result = results.get(0);
                            assertThat(result.getDocumentId()).isEqualTo(1L);
                            assertThat(result.getStatus()).isEqualTo(ResultStatus.SUCCESS);
                            assertThat(result.getMessage()).contains("The document has been successfully submitted for approval");
                        }
                );

                verify(documentRepository).findByIdWithLock(1L);
                verify(documentRepository).save(documentCaptor.capture());

                Document savedDoc = documentCaptor.getValue();
                assertThat(savedDoc.getStatus()).isEqualTo(DocumentStatus.SUBMITTED);
                assertThat(savedDoc.getHistory()).isNotEmpty();

                DocumentHistory historyRes = savedDoc.getHistory().get(0);
                assertThat(historyRes.getInitiator()).isEqualTo("Иванов И.И.");
                assertThat(historyRes.getAction()).isEqualTo(DocumentHistory.DocumentAction.SUBMIT);
                assertThat(historyRes.getComment()).isEqualTo("Отправка на согласование");
            }

            @Test
            @DisplayName("Должен успешно отправить несколько DRAFT документов")
            void shouldSubmitMultipleDraftDocuments() {
                // Given
                Document draftDocument1 = new Document();
                draftDocument1.setId(1L);
                draftDocument1.setDocumentNumber("DOC-20240225-000001");
                draftDocument1.setAuthor("Иванов И.И.");
                draftDocument1.setTitle("Тестовый документ");
                draftDocument1.setStatus(DocumentStatus.DRAFT); // После сохранения статус SUBMITTED
                draftDocument1.setCreatedAt(now);
                draftDocument1.setUpdatedAt(now);

                Document draftDocument2 = new Document();
                draftDocument2.setId(2L);
                draftDocument2.setDocumentNumber("DOC-20240225-000001");
                draftDocument2.setAuthor("Иванов И.И.");
                draftDocument2.setTitle("Тестовый документ");
                draftDocument2.setStatus(DocumentStatus.DRAFT); // После сохранения статус SUBMITTED
                draftDocument2.setCreatedAt(now);
                draftDocument2.setUpdatedAt(now);

                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(Arrays.asList(1L, 2L));
                request.setInitiator("Иванов И.И.");
                request.setComment("Массовая отправка");

                when(documentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(draftDocument1));
                when(documentRepository.findByIdWithLock(2L)).thenReturn(Optional.of(draftDocument2));
                when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));

                // When
                List<BatchOperationResult> results = documentService.submitDocuments(request);

                // Выводим результаты для диагностики
                results.forEach(r ->
                        System.out.println("ID: " + r.getDocumentId() +
                                ", Status: " + r.getStatus() +
                                ", Message: " + r.getMessage())
                );

                // Then
                assertThat(results).hasSize(2);
                assertThat(results).allMatch(r -> r.getStatus() == ResultStatus.SUCCESS);

                verify(documentRepository, times(2)).findByIdWithLock(anyLong());
                verify(documentRepository, times(2)).save(any(Document.class));
            }
        }

        @Nested
        @DisplayName("Сценарии с ошибками")
        class ErrorScenariosSubmit {

            @Test
            @DisplayName("Должен вернуть NOT_FOUND для несуществующего документа")
            void shouldReturnNotFoundForMissingDocument() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(List.of(999L));
                request.setInitiator("Иванов И.И.");

                when(documentRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

                // When
                List<BatchOperationResult> results = documentService.submitDocuments(request);

                // Then
                assertThat(results).hasSize(1);
                BatchOperationResult result = results.get(0);

                assertAll("Проверка NOT_FOUND",
                        () -> assertThat(result.getDocumentId()).isEqualTo(999L),
                        () -> assertThat(result.getStatus()).isEqualTo(ResultStatus.NOT_FOUND),
                        () -> assertThat(result.getMessage()).contains("Document with ID 999 not found")
                );

                verify(documentRepository, never()).save(any(Document.class));
            }

            @Test
            @DisplayName("Должен вернуть CONFLICT для документа не в статусе DRAFT")
            void shouldReturnConflictForNonDraftDocument() {
                // Given
                Document document1 = new Document();
                document1.setId(1L);
                document1.setDocumentNumber("DOC-20240225-000001");
                document1.setAuthor("Иванов И.И.");
                document1.setTitle("Тестовый документ");
                document1.setStatus(DocumentStatus.DRAFT);
                document1.setCreatedAt(now);
                document1.setUpdatedAt(now);

                Document document2 = new Document();
                document2.setId(2L);
                document2.setDocumentNumber("DOC-20240225-000001");
                document2.setAuthor("Иванов И.И.");
                document2.setTitle("Тестовый документ");
                document2.setStatus(DocumentStatus.SUBMITTED);
                document2.setCreatedAt(now);
                document2.setUpdatedAt(now);

                Document document3 = new Document();
                document3.setId(2L);
                document3.setDocumentNumber("DOC-20240225-000001");
                document3.setAuthor("Иванов И.И.");
                document3.setTitle("Тестовый документ");
                document3.setStatus(DocumentStatus.APPROVED);
                document3.setCreatedAt(now);
                document3.setUpdatedAt(now);

                BatchOperationRequest request = new BatchOperationRequest();

                request.setDocumentIds(Arrays.asList(1L, 2L, 3L));
                request.setInitiator("Иванов И.И.");

                when(documentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(document1));
                when(documentRepository.findByIdWithLock(2L)).thenReturn(Optional.of(document2));
                when(documentRepository.findByIdWithLock(3L)).thenReturn(Optional.of(document3));

                // When
                List<BatchOperationResult> results = documentService.submitDocuments(request);

                // Then
                assertThat(results).hasSize(3);

                assertThat(results).satisfiesExactly(
                        r -> {
                            assertThat(r.getDocumentId()).isEqualTo(1L);
                            assertThat(r.getStatus()).isEqualTo(ResultStatus.SUCCESS);
                        },
                        r -> {
                            assertThat(r.getDocumentId()).isEqualTo(2L);
                            assertThat(r.getStatus()).isEqualTo(ResultStatus.CONFLICT);
                            assertThat(r.getMessage()).contains("SUBMITTED");
                        },
                        r -> {
                            assertThat(r.getDocumentId()).isEqualTo(3L);
                            assertThat(r.getStatus()).isEqualTo(ResultStatus.CONFLICT);
                            assertThat(r.getMessage()).contains("APPROVED");
                        }
                );

                verify(documentRepository, times(1)).save(any(Document.class));
            }
        }
    }

    @Nested
    @DisplayName("Тесты утверждения документа")
    class ApproveDocumentTests {

        @Nested
        @DisplayName("Успешные сценарии утверждения")
        class SuccessScenariosApprove {

            @Test
            @DisplayName("Должен успешно утвердить один SUBMITTED документ")
            void shouldApproveSingleSubmittedDocument() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(List.of(2L));
                request.setInitiator("Иванов И.И.");
                request.setComment("Утверждение документа");

                when(documentRepository.findByIdWithLock(2L)).thenReturn(Optional.of(document2));
                when(registerService.createRegisterEntry(document2, "Иванов И.И.")).thenReturn(registerEntry);
                when(documentRepository.save(any(Document.class))).thenReturn(document2);

                // When
                List<BatchOperationResult> results = documentService.approveDocuments(request);

                // Then
                assertAll("Проверка результата утверждения",
                        () -> assertThat(results).hasSize(1),
                        () -> {
                            BatchOperationResult result = results.get(0);
                            assertThat(result.getDocumentId()).isEqualTo(2L);
                            assertThat(result.getStatus()).isEqualTo(ResultStatus.SUCCESS);
                            assertThat(result.getMessage()).contains("The document has been successfully approved");
                            assertThat(result.getRegistrationNumber()).isEqualTo("REG-20240305-001");
                        }
                );

                verify(documentRepository).findByIdWithLock(2L);
                verify(registerService).createRegisterEntry(document2, "Иванов И.И.");
                verify(documentRepository).save(documentCaptor.capture());

                Document savedDoc = documentCaptor.getValue();
                assertThat(savedDoc.getStatus()).isEqualTo(DocumentStatus.APPROVED);
                assertThat(savedDoc.getHistory()).isNotEmpty();

                DocumentHistory history = savedDoc.getHistory().get(0);
                assertThat(history.getInitiator()).isEqualTo("Иванов И.И.");
                assertThat(history.getAction()).isEqualTo(DocumentHistory.DocumentAction.APPROVE);
                assertThat(history.getComment()).isEqualTo("Утверждение документа");
            }

            @Test
            @DisplayName("Должен успешно утвердить несколько SUBMITTED документов")
            void shouldApproveMultipleSubmittedDocuments() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(Arrays.asList(2L, 5L));
                request.setInitiator("Иванов И.И.");
                request.setComment("Массовое утверждение");

                Document document5 = new Document();
                document5.setId(5L);
                document5.setDocumentNumber("DOC-20240225-000005");
                document5.setAuthor("Николаев Н.Н.");
                document5.setTitle("Документ 5");
                document5.setStatus(DocumentStatus.SUBMITTED);
                document5.setCreatedAt(now.minusDays(3));
                document5.setUpdatedAt(now.minusDays(3));

                ApprovalRegister registerEntry5 = ApprovalRegister.builder()
                        .id(101L)
                        .document(document5)
                        .approvedBy("Иванов И.И.")
                        .registrationNumber("REG-20240305-002")
                        .approvedAt(now)
                        .build();

                when(documentRepository.findByIdWithLock(2L)).thenReturn(Optional.of(document2));
                when(documentRepository.findByIdWithLock(5L)).thenReturn(Optional.of(document5));
                when(registerService.createRegisterEntry(document2, "Иванов И.И.")).thenReturn(registerEntry);
                when(registerService.createRegisterEntry(document5, "Иванов И.И.")).thenReturn(registerEntry5);
                when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));

                // When
                List<BatchOperationResult> results = documentService.approveDocuments(request);

                // Then
                assertThat(results).hasSize(2);
                assertThat(results).allMatch(r -> r.getStatus() == ResultStatus.SUCCESS);

                verify(documentRepository, times(2)).findByIdWithLock(anyLong());
                verify(registerService, times(2)).createRegisterEntry(any(Document.class), eq("Иванов И.И."));
                verify(documentRepository, times(2)).save(any(Document.class));
            }
        }

        @Nested
        @DisplayName("Сценарии с ошибками")
        class ErrorScenariosApprove {

            @Test
            @DisplayName("Должен вернуть NOT_FOUND для несуществующего документа")
            void shouldReturnNotFoundForMissingDocument() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(List.of(999L));
                request.setInitiator("Иванов И.И.");

                when(documentRepository.findByIdWithLock(999L)).thenReturn(Optional.empty());

                // When
                List<BatchOperationResult> results = documentService.approveDocuments(request);

                // Then
                assertThat(results).hasSize(1);
                BatchOperationResult result = results.get(0);

                assertAll("Проверка NOT_FOUND",
                        () -> assertThat(result.getDocumentId()).isEqualTo(999L),
                        () -> assertThat(result.getStatus()).isEqualTo(ResultStatus.NOT_FOUND),
                        () -> assertThat(result.getMessage()).contains("Document with ID 999 not found")
                );

                verify(registerService, never()).createRegisterEntry(any(), any());
                verify(documentRepository, never()).save(any());
            }

            @Test
            @DisplayName("Должен вернуть CONFLICT для документа не в статусе SUBMITTED")
            void shouldReturnConflictForNonSubmittedDocument() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(Arrays.asList(1L, 2L, 3L, 4L));
                request.setInitiator("Иванов И.И.");

                when(documentRepository.findByIdWithLock(1L)).thenReturn(Optional.of(document));   // DRAFT
                when(documentRepository.findByIdWithLock(2L)).thenReturn(Optional.of(document2)); // SUBMITTED
                when(documentRepository.findByIdWithLock(3L)).thenReturn(Optional.of(document3)); // APPROVED
                when(documentRepository.findByIdWithLock(4L)).thenReturn(Optional.of(document4)); // DRAFT
                when(registerService.createRegisterEntry(document2, "Иванов И.И.")).thenReturn(registerEntry);

                // When
                List<BatchOperationResult> results = documentService.approveDocuments(request);

                // Then
                assertThat(results).hasSize(4);

                assertAll("Проверка статусов",
                        () -> assertThat(results.get(0).getStatus()).isEqualTo(ResultStatus.CONFLICT), // DRAFT
                        () -> assertThat(results.get(1).getStatus()).isEqualTo(ResultStatus.SUCCESS),  // SUBMITTED
                        () -> assertThat(results.get(2).getStatus()).isEqualTo(ResultStatus.CONFLICT), // APPROVED
                        () -> assertThat(results.get(3).getStatus()).isEqualTo(ResultStatus.CONFLICT)  // DRAFT
                );

                verify(registerService, times(1)).createRegisterEntry(any(), any());
                verify(documentRepository, times(1)).save(any());
            }

            @Test
            @DisplayName("Должен вернуть REGISTRATION_ERROR при ошибке создания записи в реестре")
            void shouldReturnRegistrationErrorWhenRegisterFails() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(List.of(2L));
                request.setInitiator("Иванов И.И.");

                when(documentRepository.findByIdWithLock(2L)).thenReturn(Optional.of(document2));
                when(registerService.createRegisterEntry(document2, "Иванов И.И."))
                        .thenThrow(new RuntimeException("Ошибка подключения к реестру"));

                // When
                List<BatchOperationResult> results = documentService.approveDocuments(request);

                // Then
                assertThat(results).hasSize(1);
                BatchOperationResult result = results.get(0);

                assertAll("Проверка REGISTRATION_ERROR",
                        () -> assertThat(result.getDocumentId()).isEqualTo(2L),
                        () -> assertThat(result.getStatus()).isEqualTo(ResultStatus.REGISTRATION_ERROR),
                        () -> assertThat(result.getMessage()).contains("Ошибка регистрации")
                );

                verify(documentRepository, never()).save(any());
            }
        }

        @Nested
        @DisplayName("Проверка атомарности")
        class AtomicityTest {

            @Test
            @DisplayName("Ошибка в одном документе не влияет на обработку других")
            void errorInOneDocumentShouldNotAffectOthers() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(Arrays.asList(2L, 5L));
                request.setInitiator("Иванов И.И.");

                Document document5 = new Document();
                document5.setId(5L);
                document5.setStatus(DocumentStatus.SUBMITTED);

                when(documentRepository.findByIdWithLock(2L)).thenReturn(Optional.of(document2));
                when(documentRepository.findByIdWithLock(5L)).thenThrow(new RuntimeException("Connection error"));

                // When
                List<BatchOperationResult> results = documentService.approveDocuments(request);

                // Then
                assertThat(results).hasSize(2);

                assertAll("Проверка атомарности",
                        () -> assertThat(results.get(0).getStatus()).isEqualTo(ResultStatus.CONFLICT), // ошибка в сервисе
                        () -> assertThat(results.get(1).getStatus()).isEqualTo(ResultStatus.CONFLICT)  // исключение
                );
            }
        }

        @Nested
        @DisplayName("Проверка истории и реестра")
        class HistoryAndRegisterTests {

            @Test
            @DisplayName("Должен создать запись в истории при успешном утверждении")
            void shouldCreateHistoryEntryOnSuccess() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(List.of(2L));
                request.setInitiator("Иванов И.И.");
                request.setComment("Тестовое утверждение");

                when(documentRepository.findByIdWithLock(2L)).thenReturn(Optional.of(document2));
                when(registerService.createRegisterEntry(document2, "Иванов И.И.")).thenReturn(registerEntry);
                when(documentRepository.save(any(Document.class))).thenReturn(document2);

                // When
                documentService.approveDocuments(request);

                // Then
                verify(documentRepository).save(documentCaptor.capture());
                Document savedDoc = documentCaptor.getValue();

                assertThat(savedDoc.getHistory()).hasSize(1);
                DocumentHistory history = savedDoc.getHistory().get(0);

                assertAll("Проверка записи истории",
                        () -> assertThat(history.getInitiator()).isEqualTo("Иванов И.И."),
                        () -> assertThat(history.getAction()).isEqualTo(DocumentHistory.DocumentAction.APPROVE),
                        () -> assertThat(history.getComment()).isEqualTo("Тестовое утверждение"),
                        () -> assertThat(history.getDocument()).isEqualTo(savedDoc)
                );
            }

            @Test
            @DisplayName("Должен создать запись в реестре при успешном утверждении")
            void shouldCreateRegisterEntryOnSuccess() {
                // Given
                BatchOperationRequest request = new BatchOperationRequest();
                request.setDocumentIds(List.of(2L));
                request.setInitiator("Иванов И.И.");

                when(documentRepository.findByIdWithLock(2L)).thenReturn(Optional.of(document2));
                when(registerService.createRegisterEntry(document2, "Иванов И.И.")).thenReturn(registerEntry);
                when(documentRepository.save(any(Document.class))).thenReturn(document2);

                // When
                List<BatchOperationResult> results = documentService.approveDocuments(request);

                // Then
                verify(registerService).createRegisterEntry(document2, "Иванов И.И.");

                assertThat(results.get(0).getRegistrationNumber()).isEqualTo("REG-20240305-001");
            }
        }
    }

    @Nested
    @DisplayName("Тесты поиска документов")
    class SearchDocumentTests {

        @Test
        @DisplayName("Должен найти документы по статусу DRAFT")
        void shouldFindDocumentsByDraftStatus() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(DocumentStatus.DRAFT);

            List<Document> draftDocuments = Arrays.asList(document, document4);

            when(searchRepository.findByStatus(DocumentStatus.DRAFT)).thenReturn(draftDocuments);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);
            when(documentMapper.mapToDocumentResponse(document4)).thenReturn(documentResponse4);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertAll("Проверка поиска по статусу DRAFT",
                    () -> assertThat(results).hasSize(2),
                    () -> assertThat(results).extracting(DocumentResponse::getStatus)
                            .allMatch(s -> s == DocumentStatus.DRAFT),
                    () -> assertThat(results).extracting(DocumentResponse::getDocumentNumber)
                            .containsExactlyInAnyOrder("DOC-20240225-000001", "DOC-20240225-000004")
            );

            verify(searchRepository).findByStatus(DocumentStatus.DRAFT);
            verify(documentMapper, times(2)).mapToDocumentResponse(any(Document.class));
        }

        @Test
        @DisplayName("Должен найти документы по автору (частичное совпадение)")
        void shouldFindDocumentsByAuthorPartialMatch() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setAuthor("Андреев");

            List<Document> authorDocuments = List.of(document2);

            when(searchRepository.findByAuthorContainingIgnoreCase("Андреев")).thenReturn(authorDocuments);
            when(documentMapper.mapToDocumentResponse(document2)).thenReturn(documentResponse2);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertAll("Проверка поиска по автору",
                    () -> assertThat(results).hasSize(1),
                    () -> assertThat(results.get(0).getAuthor()).isEqualTo("Андреев А.А.")
            );

            verify(searchRepository).findByAuthorContainingIgnoreCase("Андреев");
        }

        @Test
        @DisplayName("Должен найти документы по периоду дат")
        void shouldFindDocumentsByDateRange() {
            // Given
            LocalDateTime from = now.minusDays(3);
            LocalDateTime to = now;

            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setDateFrom(from);
            request.setDateTo(to);

            List<Document> dateRangeDocuments = Arrays.asList(document, document3, document4);

            when(searchRepository.findByCreatedAtBetween(from, to)).thenReturn(dateRangeDocuments);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);
            when(documentMapper.mapToDocumentResponse(document3)).thenReturn(documentResponse3);
            when(documentMapper.mapToDocumentResponse(document4)).thenReturn(documentResponse4);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertAll("Проверка поиска по периоду",
                    () -> assertThat(results).hasSize(3),
                    () -> assertThat(results).extracting(DocumentResponse::getDocumentNumber)
                            .containsExactlyInAnyOrder("DOC-20240225-000001", "DOC-20240225-000003", "DOC-20240225-000004")
            );

            verify(searchRepository).findByCreatedAtBetween(from, to);
        }

        @Test
        @DisplayName("Должен найти документы по статусу и автору")
        void shouldFindDocumentsByStatusAndAuthor() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(DocumentStatus.DRAFT);
            request.setAuthor("Иванов И.И.");

            List<Document> result = List.of(document);

            when(searchRepository.findByStatusAndAuthor(DocumentStatus.DRAFT, "Иванов И.И."))
                    .thenReturn(result);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertAll("Проверка поиска по статусу и автору",
                    () -> assertThat(results).hasSize(1),
                    () -> assertThat(results.get(0).getStatus()).isEqualTo(DocumentStatus.DRAFT),
                    () -> assertThat(results.get(0).getAuthor()).isEqualTo("Иванов И.И.")
            );

            verify(searchRepository).findByStatusAndAuthor(DocumentStatus.DRAFT, "Иванов И.И.");
        }

        @Test
        @DisplayName("Должен найти документы по статусу и периоду")
        void shouldFindDocumentsByStatusAndDateRange() {
            // Given
            LocalDateTime from = now.minusDays(3);
            LocalDateTime to = now;

            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(DocumentStatus.DRAFT);
            request.setDateFrom(from);
            request.setDateTo(to);

            List<Document> result = Arrays.asList(document, document4);

            when(searchRepository.findByStatusAndCreatedAtBetween(DocumentStatus.DRAFT, from, to))
                    .thenReturn(result);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);
            when(documentMapper.mapToDocumentResponse(document4)).thenReturn(documentResponse4);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertAll("Проверка поиска по статусу и периоду",
                    () -> assertThat(results).hasSize(2),
                    () -> assertThat(results).allMatch(r -> r.getStatus() == DocumentStatus.DRAFT)
            );
        }

        @Test
        @DisplayName("Должен найти документы по автору и периоду")
        void shouldFindDocumentsByAuthorAndDateRange() {
            // Given
            LocalDateTime from = now.minusDays(3);
            LocalDateTime to = now;

            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setAuthor("Иванов И.И.");
            request.setDateFrom(from);
            request.setDateTo(to);

            List<Document> result = List.of(document);

            when(searchRepository.findByAuthorAndCreatedAtBetween("Иванов И.И.", from, to))
                    .thenReturn(result);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertAll("Проверка поиска по автору и периоду",
                    () -> assertThat(results).hasSize(1),
                    () -> assertThat(results.get(0).getAuthor()).isEqualTo("Иванов И.И.")
            );
        }

        @Test
        @DisplayName("Должен найти документы по всем параметрам")
        void shouldFindDocumentsByAllParameters() {
            // Given
            LocalDateTime from = now.minusDays(3);
            LocalDateTime to = now;

            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(DocumentStatus.DRAFT);
            request.setAuthor("Иванов И.И.");
            request.setDateFrom(from);
            request.setDateTo(to);

            List<Document> result = List.of(document);

            when(searchRepository.findByStatusAndAuthorAndCreatedAtBetween(
                    DocumentStatus.DRAFT, "Иванов И.И.", from, to))
                    .thenReturn(result);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertAll("Проверка поиска по всем параметрам",
                    () -> assertThat(results).hasSize(1),
                    () -> assertThat(results.get(0).getStatus()).isEqualTo(DocumentStatus.DRAFT),
                    () -> assertThat(results.get(0).getAuthor()).isEqualTo("Иванов И.И."),
                    () -> assertThat(results.get(0).getCreatedAt()).isBetween(from, to)
            );

            verify(searchRepository).findByStatusAndAuthorAndCreatedAtBetween(
                    DocumentStatus.DRAFT, "Иванов И.И.", from, to);
        }

        @Test
        @DisplayName("Должен вернуть все документы когда нет фильтров")
        void shouldReturnAllDocumentsWhenNoFilters() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();

            List<Document> allDocuments = Arrays.asList(document, document2, document3, document4);

            when(documentRepository.findAll()).thenReturn(allDocuments);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);
            when(documentMapper.mapToDocumentResponse(document2)).thenReturn(documentResponse2);
            when(documentMapper.mapToDocumentResponse(document3)).thenReturn(documentResponse3);
            when(documentMapper.mapToDocumentResponse(document4)).thenReturn(documentResponse4);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertAll("Проверка поиска без фильтров",
                    () -> assertThat(results).hasSize(4),
                    () -> assertThat(results).extracting(DocumentResponse::getDocumentNumber)
                            .containsExactlyInAnyOrder(
                                    "DOC-20240225-000001",
                                    "DOC-20240225-000002",
                                    "DOC-20240225-000003",
                                    "DOC-20240225-000004"
                            )
            );

            verify(documentRepository).findAll();
            verify(documentMapper, times(4)).mapToDocumentResponse(any(Document.class));
        }

        @Test
        @DisplayName("Должен вернуть пустой список если ничего не найдено")
        void shouldReturnEmptyListWhenNothingFound() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setAuthor("Несуществующий Автор");

            when(searchRepository.findByAuthorContainingIgnoreCase("Несуществующий Автор"))
                    .thenReturn(Collections.emptyList());

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertAll("Проверка пустого результата",
                    () -> assertThat(results).isNotNull(),
                    () -> assertThat(results).isEmpty()
            );

            verify(documentMapper, never()).mapToDocumentResponse(any());
        }
    }

    @Nested
    @DisplayName("Тесты поиска документов с null значениями")
    class SearchDocumentsWithNullValueTests {

        @Test
        @DisplayName("Должен вызвать findAll при всех null параметрах")
        void shouldCallFindAllWhenAllParamsNull() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(null);
            request.setAuthor(null);
            request.setDateFrom(null);
            request.setDateTo(null);

            List<Document> allDocuments = Arrays.asList(document, document2, document3, document4);

            when(documentRepository.findAll()).thenReturn(allDocuments);
            when(documentMapper.mapToDocumentResponse(any())).thenReturn(
                    documentResponse, documentResponse2, documentResponse3, documentResponse4);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertThat(results).hasSize(4);
            verify(documentRepository).findAll();
            verify(searchRepository, never()).findByStatus(any());
        }

        @Test
        @DisplayName("Должен вызвать findByStatus когда автор и период null")
        void shouldCallFindByStatusWhenAuthorAndDateRangeNull() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(DocumentStatus.DRAFT);
            request.setAuthor(null);
            request.setDateFrom(null);
            request.setDateTo(null);

            List<Document> draftDocuments = Arrays.asList(document, document4);

            when(searchRepository.findByStatus(DocumentStatus.DRAFT)).thenReturn(draftDocuments);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);
            when(documentMapper.mapToDocumentResponse(document4)).thenReturn(documentResponse4);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertThat(results).hasSize(2);
            verify(searchRepository).findByStatus(DocumentStatus.DRAFT);
            verify(searchRepository, never()).findByStatusAndAuthor(any(), any());
        }

        @Test
        @DisplayName("Должен вызвать findByAuthorContainingIgnoreCase когда статус и период null")
        void shouldCallFindByAuthorWhenStatusAndDateRangeNull() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(null);
            request.setAuthor("Иванов");
            request.setDateFrom(null);
            request.setDateTo(null);

            List<Document> authorDocuments = List.of(document);

            when(searchRepository.findByAuthorContainingIgnoreCase("Иванов")).thenReturn(authorDocuments);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertThat(results).hasSize(1);
            verify(searchRepository).findByAuthorContainingIgnoreCase("Иванов");
        }

        @Test
        @DisplayName("Должен вызвать findByCreatedAtBetween когда статус и автор null, но период полный")
        void shouldCallFindByDateRangeWhenStatusAndAuthorNullButDateRangeFull() {
            // Given
            LocalDateTime from = now.minusDays(3);
            LocalDateTime to = now;

            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(null);
            request.setAuthor(null);
            request.setDateFrom(from);
            request.setDateTo(to);

            List<Document> dateRangeDocuments = Arrays.asList(document, document3, document4);

            when(searchRepository.findByCreatedAtBetween(from, to)).thenReturn(dateRangeDocuments);
            when(documentMapper.mapToDocumentResponse(document)).thenReturn(documentResponse);
            when(documentMapper.mapToDocumentResponse(document3)).thenReturn(documentResponse3);
            when(documentMapper.mapToDocumentResponse(document4)).thenReturn(documentResponse4);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertThat(results).hasSize(3);
            verify(searchRepository).findByCreatedAtBetween(from, to);
        }

        @Test
        @DisplayName("Должен вызвать findAll когда период неполный (только from)")
        void shouldCallFindAllWhenDateRangeIncompleteWithOnlyFrom() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(null);
            request.setAuthor(null);
            request.setDateFrom(now.minusDays(3));
            request.setDateTo(null); // dateTo is null

            List<Document> allDocuments = Arrays.asList(document, document2, document3, document4);

            when(documentRepository.findAll()).thenReturn(allDocuments);
            when(documentMapper.mapToDocumentResponse(any())).thenReturn(
                    documentResponse, documentResponse2, documentResponse3, documentResponse4);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertThat(results).hasSize(4);
            verify(documentRepository).findAll();
            verify(searchRepository, never()).findByCreatedAtBetween(any(), any());
        }

        @Test
        @DisplayName("Должен вызвать findAll когда период неполный (только to)")
        void shouldCallFindAllWhenDateRangeIncompleteWithOnlyTo() {
            // Given
            DocumentSearchRequest request = new DocumentSearchRequest();
            request.setStatus(null);
            request.setAuthor(null);
            request.setDateFrom(null);
            request.setDateTo(now);

            List<Document> allDocuments = Arrays.asList(document, document2, document3, document4);

            when(documentRepository.findAll()).thenReturn(allDocuments);
            when(documentMapper.mapToDocumentResponse(any())).thenReturn(
                    documentResponse, documentResponse2, documentResponse3, documentResponse4);

            // When
            List<DocumentResponse> results = documentService.searchDocuments(request);

            // Then
            assertThat(results).hasSize(4);
            verify(documentRepository).findAll();
        }
    }
}
