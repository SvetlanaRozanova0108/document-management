package ru.practicum.web.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import ru.practicum.web.dto.document.DocumentHistoryDto;
import ru.practicum.web.mapper.DocumentHistoryMapper;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.DocumentHistory;
import ru.practicum.web.model.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DisplayName("Тесты DocumentHistoryMapper")
class DocumentHistoryMapperTest {

    @Autowired
    private DocumentHistoryMapper historyMapper;

    private Document document;
    private DocumentHistory history1;
    private DocumentHistory history2;
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

        // Создаем первую запись истории
        history1 = new DocumentHistory();
        history1.setId(1L);
        history1.setDocument(document);
        history1.setInitiator("Иванов И.И.");
        history1.setAction(DocumentHistory.DocumentAction.SUBMIT);
        history1.setComment("Создание документа");
        history1.setCreatedAt(now.minusDays(1));

        // Создаем вторую запись истории
        history2 = new DocumentHistory();
        history2.setId(2L);
        history2.setDocument(document);
        history2.setInitiator("Петров П.П.");
        history2.setAction(DocumentHistory.DocumentAction.APPROVE);
        history2.setComment("Утверждение документа");
        history2.setCreatedAt(now);
    }

    @Nested
    @DisplayName("Тесты метода mapToDocumentHistoryDto")
    class MapToDocumentHistoryDtoTests {

        @Test
        @DisplayName("Должен преобразовать DocumentHistory в DocumentHistoryDto со всеми полями")
        void shouldMapHistoryToDto() {
            // When
            DocumentHistoryDto dto = historyMapper.mapToDocumentHistoryDto(history1);

            // Then
            assertAll("Проверка всех полей DocumentHistoryDto",
                    () -> assertThat(dto).isNotNull(),
                    () -> assertThat(dto.getInitiator()).isEqualTo(history1.getInitiator()),
                    () -> assertThat(dto.getAction()).isEqualTo(history1.getAction()),
                    () -> assertThat(dto.getComment()).isEqualTo(history1.getComment()),
                    () -> assertThat(dto.getCreatedAt()).isEqualTo(history1.getCreatedAt())
            );
        }

        @Test
        @DisplayName("Должен игнорировать поле id при маппинге")
        void shouldIgnoreIdField() {
            // Given
            assertThat(history1.getId()).isEqualTo(1L);

            // When
            DocumentHistoryDto dto = historyMapper.mapToDocumentHistoryDto(history1);

            // Then
            assertThat(dto).isNotNull();
            // В DocumentHistoryDto нет поля id, проверяем что остальные поля маппятся
            assertThat(dto.getInitiator()).isEqualTo(history1.getInitiator());
            assertThat(dto.getAction()).isEqualTo(history1.getAction());
        }

        @Test
        @DisplayName("Должен корректно обрабатывать пустой comment")
        void shouldHandleEmptyComment() {
            // Given
            history1.setComment("");

            // When
            DocumentHistoryDto dto = historyMapper.mapToDocumentHistoryDto(history1);

            // Then
            assertThat(dto.getComment()).isEmpty();
        }

        @Test
        @DisplayName("Должен возвращать null при передаче null")
        void shouldReturnNullWhenHistoryIsNull() {
            // When
            DocumentHistoryDto dto = historyMapper.mapToDocumentHistoryDto(null);

            // Then
            assertThat(dto).isNull();
        }
    }

    @Nested
    @DisplayName("Тесты метода mapToHistoryDtoList")
    class MapToHistoryDtoListTests {

        @Test
        @DisplayName("Должен преобразовать список DocumentHistory в список DocumentHistoryDto")
        void shouldMapHistoryListToDtoList() {
            // Given
            List<DocumentHistory> histories = Arrays.asList(history1, history2);

            // When
            List<DocumentHistoryDto> dtos = historyMapper.mapToHistoryDtoList(histories);

            // Then
            assertAll("Проверка списка DTO",
                    () -> assertThat(dtos).isNotNull(),
                    () -> assertThat(dtos).hasSize(2),
                    () -> {
                        DocumentHistoryDto dto1 = dtos.get(0);
                        assertThat(dto1.getInitiator()).isEqualTo(history1.getInitiator());
                        assertThat(dto1.getAction()).isEqualTo(history1.getAction());
                        assertThat(dto1.getComment()).isEqualTo(history1.getComment());
                        assertThat(dto1.getCreatedAt()).isEqualTo(history1.getCreatedAt());
                    },
                    () -> {
                        DocumentHistoryDto dto2 = dtos.get(1);
                        assertThat(dto2.getInitiator()).isEqualTo(history2.getInitiator());
                        assertThat(dto2.getAction()).isEqualTo(history2.getAction());
                        assertThat(dto2.getComment()).isEqualTo(history2.getComment());
                        assertThat(dto2.getCreatedAt()).isEqualTo(history2.getCreatedAt());
                    }
            );
        }

        @Test
        @DisplayName("Должен корректно обрабатывать список с null элементами")
        void shouldHandleListWithNullElements() {
            // Given
            List<DocumentHistory> histories = Arrays.asList(history1, null, history2);

            // When
            List<DocumentHistoryDto> dtos = historyMapper.mapToHistoryDtoList(histories);

            // Then
            assertThat(dtos).hasSize(3);
            assertThat(dtos.get(0)).isNotNull();
            assertThat(dtos.get(1)).isNull();
            assertThat(dtos.get(2)).isNotNull();

            assertThat(dtos.get(0).getInitiator()).isEqualTo(history1.getInitiator());
            assertThat(dtos.get(2).getInitiator()).isEqualTo(history2.getInitiator());
        }

        @Test
        @DisplayName("Должен обрабатывать список с одним элементом")
        void shouldHandleSingleElementList() {
            // Given
            List<DocumentHistory> singleList = List.of(history1);

            // When
            List<DocumentHistoryDto> dtos = historyMapper.mapToHistoryDtoList(singleList);

            // Then
            assertThat(dtos).hasSize(1);
            assertThat(dtos.get(0).getInitiator()).isEqualTo(history1.getInitiator());
        }
    }

    @Nested
    @DisplayName("Тесты метода mapToDocumentHistory")
    class MapToDocumentHistoryTests {

        @Test
        @DisplayName("Должен преобразовать DocumentHistoryDto в DocumentHistory")
        void shouldMapDtoToHistory() {
            // Given
            DocumentHistoryDto dto = DocumentHistoryDto.builder()
                    .initiator("Иванов И.И.")
                    .action(DocumentHistory.DocumentAction.SUBMIT)
                    .comment("Создание документа")
                    .createdAt(now.minusDays(1))
                    .build();

            // When
            DocumentHistory history = historyMapper.mapToDocumentHistory(dto);

            // Then
            assertAll("Проверка всех полей DocumentHistory",
                    () -> assertThat(history).isNotNull(),
                    () -> assertThat(history.getId()).isNull(), // ID не маппится
                    () -> assertThat(history.getInitiator()).isEqualTo(dto.getInitiator()),
                    () -> assertThat(history.getAction()).isEqualTo(dto.getAction()),
                    () -> assertThat(history.getComment()).isEqualTo(dto.getComment()),
                    () -> assertThat(history.getCreatedAt()).isEqualTo(dto.getCreatedAt()),
                    () -> assertThat(history.getDocument()).isNull() // Document не маппится
            );
        }

        @Test
        @DisplayName("Должен игнорировать отсутствующие поля при маппинге")
        void shouldIgnoreMissingFields() {
            // Given
            DocumentHistoryDto dto = DocumentHistoryDto.builder()
                    .initiator("Иванов И.И.")
                    .action(DocumentHistory.DocumentAction.SUBMIT)
                    .build();
            // comment и createdAt отсутствуют

            // When
            DocumentHistory history = historyMapper.mapToDocumentHistory(dto);

            // Then
            assertAll("Проверка маппинга с отсутствующими полями",
                    () -> assertThat(history).isNotNull(),
                    () -> assertThat(history.getInitiator()).isEqualTo("Иванов И.И."),
                    () -> assertThat(history.getAction()).isEqualTo(DocumentHistory.DocumentAction.SUBMIT),
                    () -> assertThat(history.getComment()).isNull(),
                    () -> assertThat(history.getCreatedAt()).isNull()
            );
        }

        @Test
        @DisplayName("Должен корректно обрабатывать пустой comment в DTO")
        void shouldHandleEmptyCommentInDto() {
            // Given
            DocumentHistoryDto dto = DocumentHistoryDto.builder()
                    .initiator("Иванов И.И.")
                    .action(DocumentHistory.DocumentAction.SUBMIT)
                    .comment("")
                    .createdAt(now)
                    .build();

            // When
            DocumentHistory history = historyMapper.mapToDocumentHistory(dto);

            // Then
            assertThat(history.getComment()).isEmpty();
        }
    }
}