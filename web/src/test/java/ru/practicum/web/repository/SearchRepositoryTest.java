package ru.practicum.web.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@DisplayName("Тесты SearchRepository")
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
class SearchRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SearchRepository searchRepository;

    private Document document1;
    private Document document2;
    private Document document3;
    private Document document4;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Документ 1: Иванов, DRAFT, сейчас
        document1 = new Document();
        document1.setDocumentNumber("DOC-001");
        document1.setAuthor("Иванов И.И.");
        document1.setTitle("Документ 1");
        document1.setStatus(DocumentStatus.DRAFT);
        document1.setCreatedAt(now);
        document1.setUpdatedAt(now);
        entityManager.persist(document1);

        // Документ 2: Петров, SUBMITTED, 2 дня назад
        document2 = new Document();
        document2.setDocumentNumber("DOC-002");
        document2.setAuthor("Петров П.П.");
        document2.setTitle("Документ 2");
        document2.setStatus(DocumentStatus.SUBMITTED);
        document2.setCreatedAt(now.minusDays(2));
        document2.setUpdatedAt(now.minusDays(2));
        entityManager.persist(document2);

        // Документ 3: Сидоров, APPROVED, 5 дней назад
        document3 = new Document();
        document3.setDocumentNumber("DOC-003");
        document3.setAuthor("Сидоров С.С.");
        document3.setTitle("Документ 3");
        document3.setStatus(DocumentStatus.APPROVED);
        document3.setCreatedAt(now.minusDays(5));
        document3.setUpdatedAt(now.minusDays(5));
        entityManager.persist(document3);

        // Документ 4: Иванов, DRAFT, 1 день назад
        document4 = new Document();
        document4.setDocumentNumber("DOC-004");
        document4.setAuthor("Иванов И.И.");
        document4.setTitle("Документ 4");
        document4.setStatus(DocumentStatus.DRAFT);
        document4.setCreatedAt(now.minusDays(1));
        document4.setUpdatedAt(now.minusDays(1));
        entityManager.persist(document4);

        entityManager.flush();
    }

    @Test
    @DisplayName("Поиск по статусу")
    void testFindByStatus() {
        // When
        List<Document> drafts = searchRepository.findByStatus(DocumentStatus.DRAFT);
        List<Document> submitted = searchRepository.findByStatus(DocumentStatus.SUBMITTED);
        List<Document> approved = searchRepository.findByStatus(DocumentStatus.APPROVED);

        // Then
        assertThat(drafts).hasSize(2);
        assertThat(drafts).extracting(Document::getDocumentNumber)
                .containsExactlyInAnyOrder("DOC-001", "DOC-004");

        assertThat(submitted).hasSize(1);
        assertThat(submitted.get(0).getDocumentNumber()).isEqualTo("DOC-002");

        assertThat(approved).hasSize(1);
        assertThat(approved.get(0).getDocumentNumber()).isEqualTo("DOC-003");
    }

    @Test
    @DisplayName("Поиск по автору (точное совпадение)")
    void testFindByAuthor() {
        // When
        List<Document> ivanovDocs = searchRepository.findByAuthor("Иванов И.И.");

        // Then
        assertThat(ivanovDocs).hasSize(2);
        assertThat(ivanovDocs).extracting(Document::getDocumentNumber)
                .containsExactlyInAnyOrder("DOC-001", "DOC-004");
    }

    @Test
    @DisplayName("Поиск по автору (частичное совпадение)")
    void testFindByAuthorContaining() {
        // When
        List<Document> ivanovDocs = searchRepository.findByAuthorContainingIgnoreCase("Иванов");
        List<Document> petrovDocs = searchRepository.findByAuthorContainingIgnoreCase("Петров");

        // Then
        assertThat(ivanovDocs).hasSize(2);
        assertThat(petrovDocs).hasSize(1);
        assertThat(petrovDocs.get(0).getDocumentNumber()).isEqualTo("DOC-002");
    }

    @Test
    @DisplayName("Поиск по периоду дат")
    void testFindByCreatedAtBetween() {
        // Given
        LocalDateTime from = now.minusDays(3);
        LocalDateTime to = now;

        // When
        List<Document> recentDocs = searchRepository.findByCreatedAtBetween(from, to);

        // Then
        assertThat(recentDocs).hasSize(3);
        assertThat(recentDocs).extracting(Document::getDocumentNumber)
                .containsExactlyInAnyOrder("DOC-001", "DOC-002", "DOC-004");
    }

    @Test
    @DisplayName("Поиск по статусу и автору")
    void testFindByStatusAndAuthor() {
        // When
        List<Document> draftsByIvanov = searchRepository.findByStatusAndAuthor(
                DocumentStatus.DRAFT, "Иванов И.И.");

        // Then
        assertThat(draftsByIvanov).hasSize(2);
        assertThat(draftsByIvanov).allMatch(d -> d.getStatus() == DocumentStatus.DRAFT);
        assertThat(draftsByIvanov).allMatch(d -> d.getAuthor().equals("Иванов И.И."));
    }

    @Test
    @DisplayName("Поиск по статусу и периоду")
    void testFindByStatusAndCreatedAtBetween() {
        // Given
        LocalDateTime from = now.minusDays(3);
        LocalDateTime to = now;

        // When
        List<Document> recentDrafts = searchRepository.findByStatusAndCreatedAtBetween(
                DocumentStatus.DRAFT, from, to);

        // Then
        assertThat(recentDrafts).hasSize(2);
        assertThat(recentDrafts).allMatch(d -> d.getStatus() == DocumentStatus.DRAFT);
        assertThat(recentDrafts).extracting(Document::getDocumentNumber)
                .containsExactlyInAnyOrder("DOC-001", "DOC-004");
    }

    @Test
    @DisplayName("Поиск по автору и периоду")
    void testFindByAuthorAndCreatedAtBetween() {
        // Given
        LocalDateTime from = now.minusDays(3);
        LocalDateTime to = now;

        // When
        List<Document> recentIvanovDocs = searchRepository.findByAuthorAndCreatedAtBetween(
                "Иванов И.И.", from, to);

        // Then
        assertThat(recentIvanovDocs).hasSize(2);
        assertThat(recentIvanovDocs).allMatch(d -> d.getAuthor().equals("Иванов И.И."));
        assertThat(recentIvanovDocs).extracting(Document::getDocumentNumber)
                .containsExactlyInAnyOrder("DOC-001", "DOC-004");
    }

    @Test
    @DisplayName("Поиск по всем трем параметрам")
    void testFindByStatusAndAuthorAndCreatedAtBetween() {
        // Given
        LocalDateTime from = now.minusDays(3);
        LocalDateTime to = now;

        // When
        List<Document> result = searchRepository.findByStatusAndAuthorAndCreatedAtBetween(
                DocumentStatus.DRAFT, "Иванов И.И.", from, to);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(d -> d.getStatus() == DocumentStatus.DRAFT);
        assertThat(result).allMatch(d -> d.getAuthor().equals("Иванов И.И."));
        assertThat(result).allMatch(d -> d.getCreatedAt().isAfter(from) && d.getCreatedAt().isBefore(to));
    }

    @Test
    @DisplayName("Поиск без фильтров (все документы)")
    void testFindAll() {
        // When
        List<Document> allDocs = searchRepository.findAll();

        // Then
        assertThat(allDocs).hasSize(4);
    }
}
