package ru.practicum.web.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.enums.DocumentStatus;
import ru.practicum.web.repository.DocumentRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@DisplayName("Тесты DocumentRepository")
class DocumentRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private DocumentRepository documentRepository;

    private Document document1;
    private Document document2;
    private Document document3;
    private Document document4;
    private Document document5;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Документ 1: DRAFT, сейчас, Иванов
        document1 = new Document();
        document1.setDocumentNumber("DOC-001");
        document1.setAuthor("Иванов И.И.");
        document1.setTitle("Документ 1");
        document1.setStatus(DocumentStatus.DRAFT);
        document1.setCreatedAt(now);
        document1.setUpdatedAt(now);
        entityManager.persist(document1);

        // Документ 2: SUBMITTED, 2 дня назад, Петров
        document2 = new Document();
        document2.setDocumentNumber("DOC-002");
        document2.setAuthor("Петров П.П.");
        document2.setTitle("Документ 2");
        document2.setStatus(DocumentStatus.SUBMITTED);
        document2.setCreatedAt(now.minusDays(2));
        document2.setUpdatedAt(now.minusDays(2));
        entityManager.persist(document2);

        // Документ 3: APPROVED, 5 дней назад, Сидоров
        document3 = new Document();
        document3.setDocumentNumber("DOC-003");
        document3.setAuthor("Сидоров С.С.");
        document3.setTitle("Документ 3");
        document3.setStatus(DocumentStatus.APPROVED);
        document3.setCreatedAt(now.minusDays(5));
        document3.setUpdatedAt(now.minusDays(5));
        entityManager.persist(document3);

        // Документ 4: DRAFT, сейчас, Иванов
        document4 = new Document();
        document4.setDocumentNumber("DOC-004");
        document4.setAuthor("Иванов И.И.");
        document4.setTitle("Документ 4");
        document4.setStatus(DocumentStatus.DRAFT);
        document4.setCreatedAt(now);
        document4.setUpdatedAt(now);
        entityManager.persist(document4);

        // Документ 5: SUBMITTED, 1 день назад, Петров
        document5 = new Document();
        document5.setDocumentNumber("DOC-005");
        document5.setAuthor("Петров П.П.");
        document5.setTitle("Документ 5");
        document5.setStatus(DocumentStatus.SUBMITTED);
        document5.setCreatedAt(now.minusDays(1));
        document5.setUpdatedAt(now.minusDays(1));
        entityManager.persist(document5);

        entityManager.flush();
    }

    @Nested
    @DisplayName("Тесты метода findByIdIn с пагинацией")
    class FindByIdInTests {

        @Test
        @DisplayName("Должен найти документы по списку ID")
        void shouldFindDocumentsByIdList() {
            // Given
            List<Long> ids = Arrays.asList(document1.getId(), document3.getId(), document5.getId());
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Document> result = documentRepository.findByIdIn(ids, pageable);

            // Then
            assertAll("Проверка поиска по списку ID",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getContent()).hasSize(3),
                    () -> assertThat(result.getTotalElements()).isEqualTo(3),
                    () -> assertThat(result.getContent()).extracting(Document::getId)
                            .containsExactlyInAnyOrder(document1.getId(), document3.getId(), document5.getId())
            );
        }

        @Test
        @DisplayName("Должен найти документы по списку ID с пагинацией (первая страница)")
        void shouldFindDocumentsByIdListWithPaginationFirstPage() {
            // Given
            List<Long> ids = Arrays.asList(document1.getId(), document2.getId(), document3.getId(), document4.getId(), document5.getId());
            Pageable pageable = PageRequest.of(0, 2, Sort.by("id").ascending());

            // When
            Page<Document> result = documentRepository.findByIdIn(ids, pageable);

            // Then
            assertAll("Проверка первой страницы",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getContent()).hasSize(2),
                    () -> assertThat(result.getTotalElements()).isEqualTo(5),
                    () -> assertThat(result.getTotalPages()).isEqualTo(3),
                    () -> assertThat(result.getNumber()).isZero(),
                    () -> assertThat(result.getSize()).isEqualTo(2),
                    () -> assertThat(result.getContent().get(0).getId()).isEqualTo(document1.getId()),
                    () -> assertThat(result.getContent().get(1).getId()).isEqualTo(document2.getId())
            );
        }

        @Test
        @DisplayName("Должен найти документы по списку ID с сортировкой по убыванию")
        void shouldFindDocumentsByIdListWithSortDescending() {
            // Given
            List<Long> ids = Arrays.asList(document1.getId(), document2.getId(), document3.getId());
            Pageable pageable = PageRequest.of(0, 10, Sort.by("id").descending());

            // When
            Page<Document> result = documentRepository.findByIdIn(ids, pageable);

            // Then
            List<Document> content = result.getContent();
            assertThat(content).hasSize(3);
            assertThat(content.get(0).getId()).isGreaterThan(content.get(1).getId());
            assertThat(content.get(1).getId()).isGreaterThan(content.get(2).getId());
        }

        @Test
        @DisplayName("Должен найти документы по списку ID с сортировкой по дате создания")
        void shouldFindDocumentsByIdListWithSortByCreatedAt() {
            // Given
            List<Long> ids = Arrays.asList(document1.getId(), document2.getId(), document3.getId());
            Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());

            // When
            Page<Document> result = documentRepository.findByIdIn(ids, pageable);

            // Then
            List<Document> content = result.getContent();
            assertThat(content).hasSize(3);
            assertThat(content.get(0).getCreatedAt()).isAfterOrEqualTo(content.get(1).getCreatedAt());
            assertThat(content.get(1).getCreatedAt()).isAfterOrEqualTo(content.get(2).getCreatedAt());
        }

        @Test
        @DisplayName("Должен вернуть пустую страницу для несуществующих ID")
        void shouldReturnEmptyPageForNonExistentIds() {
            // Given
            List<Long> nonExistentIds = Arrays.asList(999L, 1000L, 1001L);
            Pageable pageable = PageRequest.of(0, 10);

            // When
            Page<Document> result = documentRepository.findByIdIn(nonExistentIds, pageable);

            // Then
            assertAll("Проверка страницы с несуществующими ID",
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.getContent()).isEmpty(),
                    () -> assertThat(result.getTotalElements()).isZero()
            );
        }

        @Test
        @DisplayName("Должен найти документы с пагинацией и сортировкой по автору")
        void shouldFindDocumentsWithPaginationAndSortByAuthor() {
            // Given
            List<Long> ids = Arrays.asList(document1.getId(), document2.getId(), document3.getId(), document4.getId(), document5.getId());
            Pageable pageable = PageRequest.of(0, 10, Sort.by("author").ascending());

            // When
            Page<Document> result = documentRepository.findByIdIn(ids, pageable);

            // Then
            List<Document> content = result.getContent();
            assertThat(content).hasSize(5);
            // Проверяем сортировку по автору (Иванов, Иванов, Петров, Петров, Сидоров)
            assertThat(content.get(0).getAuthor()).isEqualTo("Иванов И.И.");
            assertThat(content.get(1).getAuthor()).isEqualTo("Иванов И.И.");
            assertThat(content.get(2).getAuthor()).isEqualTo("Петров П.П.");
            assertThat(content.get(3).getAuthor()).isEqualTo("Петров П.П.");
            assertThat(content.get(4).getAuthor()).isEqualTo("Сидоров С.С.");
        }
    }

    @Nested
    @DisplayName("Тесты метода findByIdWithLock с пессимистической блокировкой")
    class FindByIdWithLockTests {

        @Test
        @DisplayName("Должен найти документ по ID с блокировкой")
        void shouldFindDocumentByIdWithLock() {
            // Given
            Long id = document1.getId();

            // When
            Optional<Document> result = documentRepository.findByIdWithLock(id);

            // Then
            assertAll("Проверка поиска с блокировкой",
                    () -> assertThat(result).isPresent(),
                    () -> assertThat(result.get().getId()).isEqualTo(id),
                    () -> assertThat(result.get().getDocumentNumber()).isEqualTo("DOC-001")
            );
        }

        @Test
        @DisplayName("Должен вернуть пустой Optional для несуществующего ID")
        void shouldReturnEmptyOptionalForNonExistentId() {
            // Given
            Long nonExistentId = 999L;

            // When
            Optional<Document> result = documentRepository.findByIdWithLock(nonExistentId);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
