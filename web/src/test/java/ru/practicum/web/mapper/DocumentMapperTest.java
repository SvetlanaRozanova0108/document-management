package ru.practicum.web.mapper;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.practicum.web.dto.document.DocumentRequest;
import ru.practicum.web.dto.document.DocumentResponse;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.DocumentHistory;
import ru.practicum.web.model.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Тесты DocumentMapper")
@RequiredArgsConstructor(onConstructor_ = @Autowired)
class DocumentMapperTest {

    @Autowired
    private DocumentMapper documentMapper;

    private Document document;
    private DocumentRequest documentRequest;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        // Создаем тестовый документ
        document = new Document();
        document.setId(1L);
        document.setDocumentNumber("DOC-20240225-000001");
        document.setAuthor("Иванов И.И.");
        document.setTitle("Тестовый документ");
        document.setStatus(DocumentStatus.DRAFT);
        document.setCreatedAt(now);
        document.setUpdatedAt(now);

        // Создаем историю документа
        DocumentHistory history1 = new DocumentHistory();
        history1.setId(1L);
        history1.setDocument(document);
        history1.setInitiator("Иванов И.И.");
        history1.setAction(DocumentHistory.DocumentAction.SUBMIT);
        history1.setComment("Создание документа");
        history1.setCreatedAt(now.minusDays(1));

        DocumentHistory history2 = new DocumentHistory();
        history2.setId(2L);
        history2.setDocument(document);
        history2.setInitiator("Петров П.П.");
        history2.setAction(DocumentHistory.DocumentAction.SUBMIT);
        history2.setComment("Отправка на согласование");
        history2.setCreatedAt(now);

        document.setHistory(Arrays.asList(history1, history2));

        // Создаем запрос на создание документа
        documentRequest = new DocumentRequest();
        documentRequest.setAuthor("Иванов И.И.");
        documentRequest.setTitle("Тестовый документ");
        documentRequest.setInitiator("Иванов И.И.");
    }

    @Nested
    @DisplayName("Тесты метода mapToDocumentResponse")
    class MapToDocumentResponseTests {

        @Test
        @DisplayName("Должен преобразовать Document в DocumentResponse со всеми полями")
        void shouldMapDocumentToDocumentResponse() {
            // When
            DocumentResponse response = documentMapper.mapToDocumentResponse(document);

            // Then
            assertAll("Проверка маппинга Document -> DocumentResponse",
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getDocumentNumber()).isEqualTo(document.getDocumentNumber()),
                    () -> assertThat(response.getAuthor()).isEqualTo(document.getAuthor()),
                    () -> assertThat(response.getTitle()).isEqualTo(document.getTitle()),
                    () -> assertThat(response.getStatus()).isEqualTo(document.getStatus()),
                    () -> assertThat(response.getCreatedAt()).isEqualTo(document.getCreatedAt()),
                    () -> assertThat(response.getUpdatedAt()).isEqualTo(document.getUpdatedAt()),
                    () -> assertThat(response.getHistory()).isNull() // history игнорируется
            );
        }

        @Test
        @DisplayName("Должен корректно обрабатывать null значения в Document")
        void shouldHandleNullValuesInDocument() {
            // Given
            document.setAuthor(null);
            document.setTitle(null);
            document.setDocumentNumber(null);
            document.setStatus(null);
            document.setCreatedAt(null);
            document.setUpdatedAt(null);

            // When
            DocumentResponse response = documentMapper.mapToDocumentResponse(document);

            // Then
            assertAll("Проверка null полей",
                    () -> assertThat(response).isNotNull(),
                    () -> assertThat(response.getDocumentNumber()).isNull(),
                    () -> assertThat(response.getAuthor()).isNull(),
                    () -> assertThat(response.getTitle()).isNull(),
                    () -> assertThat(response.getStatus()).isNull(),
                    () -> assertThat(response.getCreatedAt()).isNull(),
                    () -> assertThat(response.getUpdatedAt()).isNull()
            );
        }

        @Test
        @DisplayName("Должен корректно обрабатывать все статусы документа")
        void shouldHandleAllDocumentStatuses() {
            // Проверяем каждый статус
            for (DocumentStatus status : DocumentStatus.values()) {
                // Given
                document.setStatus(status);

                // When
                DocumentResponse response = documentMapper.mapToDocumentResponse(document);

                // Then
                assertThat(response.getStatus()).isEqualTo(status);
            }
        }

        @Test
        @DisplayName("Должен возвращать null при передаче null")
        void shouldReturnNullWhenDocumentIsNull() {
            // When
            DocumentResponse response = documentMapper.mapToDocumentResponse(null);

            // Then
            assertThat(response).isNull();
        }

        @Test
        @DisplayName("ID документа не должен маппиться в DocumentResponse")
        void documentIdShouldNotBeMappedToResponse() {
            // Given
            document.setId(999L);

            // When
            DocumentResponse response = documentMapper.mapToDocumentResponse(document);

            // Then
            // В DocumentResponse нет поля id, проверяем что остальные поля маппятся
            assertThat(response.getDocumentNumber()).isEqualTo(document.getDocumentNumber());
            assertThat(response.getAuthor()).isEqualTo(document.getAuthor());
        }
    }

    @Nested
    @DisplayName("Тесты метода mapToDocument")
    class MapToDocumentTests {

        @Test
        @DisplayName("Должен преобразовать DocumentRequest в Document со статусом DRAFT")
        void shouldMapDocumentRequestToDocument() {
            // When
            Document result = documentMapper.mapToDocument(documentRequest);

            // Then
            assertAll("Проверка маппинга DocumentRequest -> Document",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getId()).isNull(),
                    () -> assertThat(result.getDocumentNumber()).isNull(),
                    () -> assertThat(result.getAuthor()).isEqualTo(documentRequest.getAuthor()),
                    () -> assertThat(result.getTitle()).isEqualTo(documentRequest.getTitle()),
                    () -> assertThat(result.getStatus()).isEqualTo(DocumentStatus.DRAFT),
                    () -> assertThat(result.getCreatedAt()).isNull(),
                    () -> assertThat(result.getUpdatedAt()).isNull(),
                    () -> assertThat(result.getHistory()).isEmpty()
            );
        }

        @Test
        @DisplayName("Должен корректно обрабатывать null значения в запросе")
        void shouldHandleNullValuesInRequest() {
            // Given
            documentRequest.setAuthor(null);
            documentRequest.setTitle(null);

            // When
            Document result = documentMapper.mapToDocument(documentRequest);

            // Then
            assertAll("Проверка null полей",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getAuthor()).isNull(),
                    () -> assertThat(result.getTitle()).isNull(),
                    () -> assertThat(result.getStatus()).isEqualTo(DocumentStatus.DRAFT)
            );
        }

        @Test
        @DisplayName("Должен возвращать null при передаче null")
        void shouldReturnNullWhenRequestIsNull() {
            // When
            Document result = documentMapper.mapToDocument(null);

            // Then
            assertThat(result).isNull();
        }

        @Test
        @DisplayName("Должен игнорировать все поля, которые не маппятся")
        void shouldIgnoreAllUnmappedFields() {
            // When
            Document result = documentMapper.mapToDocument(documentRequest);

            // Then
            assertAll("Проверка что маппятся только author и title",
                    () -> assertThat(result.getAuthor()).isEqualTo(documentRequest.getAuthor()),
                    () -> assertThat(result.getTitle()).isEqualTo(documentRequest.getTitle()),
                    () -> assertThat(result.getStatus()).isEqualTo(DocumentStatus.DRAFT),
                    () -> assertThat(result.getId()).isNull(),
                    () -> assertThat(result.getDocumentNumber()).isNull(),
                    () -> assertThat(result.getCreatedAt()).isNull(),
                    () -> assertThat(result.getUpdatedAt()).isNull(),
                    () -> assertThat(result.getHistory()).isEmpty()
            );
        }
    }
}
