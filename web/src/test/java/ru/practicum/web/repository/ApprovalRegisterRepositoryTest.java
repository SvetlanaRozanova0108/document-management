package ru.practicum.web.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import ru.practicum.web.model.ApprovalRegister;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@DataJpaTest
@DisplayName("Тесты ApprovalRegisterRepository")
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
class ApprovalRegisterRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private ApprovalRegisterRepository approvalRegisterRepository;

    private Document document1;
    private Document document2;
    private Document document3;
    private ApprovalRegister register1;
    private ApprovalRegister register2;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();

        // Создаем тестовые документы
        document1 = new Document();
        document1.setDocumentNumber("DOC-001");
        document1.setAuthor("Иванов И.И.");
        document1.setTitle("Документ 1");
        document1.setStatus(DocumentStatus.APPROVED);
        document1.setCreatedAt(now.minusDays(5));
        document1.setUpdatedAt(now.minusDays(5));
        entityManager.persist(document1);

        document2 = new Document();
        document2.setDocumentNumber("DOC-002");
        document2.setAuthor("Петров П.П.");
        document2.setTitle("Документ 2");
        document2.setStatus(DocumentStatus.APPROVED);
        document2.setCreatedAt(now.minusDays(3));
        document2.setUpdatedAt(now.minusDays(3));
        entityManager.persist(document2);

        document3 = new Document();
        document3.setDocumentNumber("DOC-003");
        document3.setAuthor("Сидоров С.С.");
        document3.setTitle("Документ 3");
        document3.setStatus(DocumentStatus.SUBMITTED); // Не утвержден
        document3.setCreatedAt(now.minusDays(1));
        document3.setUpdatedAt(now.minusDays(1));
        entityManager.persist(document3);

        // Создаем записи в реестре утверждений
        register1 = ApprovalRegister.builder()
                .document(document1)
                .approvedBy("Иванов И.И.")
                .registrationNumber("REG-001-2024")
                .approvedAt(now.minusDays(5))
                .build();
        entityManager.persist(register1);

        register2 = ApprovalRegister.builder()
                .document(document2)
                .approvedBy("Петров П.П.")
                .registrationNumber("REG-002-2024")
                .approvedAt(now.minusDays(3))
                .build();
        entityManager.persist(register2);

        entityManager.flush();
    }

    @Nested
    @DisplayName("Тесты метода existsByDocumentId")
    class ExistsByDocumentIdTests {

        @Test
        @DisplayName("Должен вернуть true для документа с существующей записью в реестре")
        void shouldReturnTrueForDocumentWithExistingRegister() {
            // Given
            Long documentId = document1.getId();

            // When
            boolean exists = approvalRegisterRepository.existsByDocumentId(documentId);

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("Должен вернуть false для документа без записи в реестре")
        void shouldReturnFalseForDocumentWithoutRegister() {
            // Given
            Long documentId = document3.getId(); // Документ без записи в реестре

            // When
            boolean exists = approvalRegisterRepository.existsByDocumentId(documentId);

            // Then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("Должен вернуть false для несуществующего документа")
        void shouldReturnFalseForNonExistentDocument() {
            // Given
            Long nonExistentId = 999L;

            // When
            boolean exists = approvalRegisterRepository.existsByDocumentId(nonExistentId);

            // Then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("Тесты метода findByDocumentId")
    class FindByDocumentIdTests {

        @Test
        @DisplayName("Должен найти запись в реестре по ID документа")
        void shouldFindRegisterByDocumentId() {
            // Given
            Long documentId = document1.getId();

            // When
            Optional<ApprovalRegister> result = approvalRegisterRepository.findByDocumentId(documentId);

            // Then
            assertAll("Проверка найденной записи",
                    () -> assertThat(result).isPresent(),
                    () -> {
                        ApprovalRegister register = result.get();
                        assertThat(register.getId()).isEqualTo(register1.getId());
                        assertThat(register.getDocument().getId()).isEqualTo(document1.getId());
                        assertThat(register.getApprovedBy()).isEqualTo("Иванов И.И.");
                        assertThat(register.getRegistrationNumber()).isEqualTo("REG-001-2024");
                        assertThat(register.getApprovedAt()).isEqualTo(now.minusDays(5));
                    }
            );
        }

        @Test
        @DisplayName("Должен вернуть пустой Optional для документа без записи в реестре")
        void shouldReturnEmptyOptionalForDocumentWithoutRegister() {
            // Given
            Long documentId = document3.getId();

            // When
            Optional<ApprovalRegister> result = approvalRegisterRepository.findByDocumentId(documentId);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("Должен вернуть пустой Optional для несуществующего документа")
        void shouldReturnEmptyOptionalForNonExistentDocument() {
            // Given
            Long nonExistentId = 999L;

            // When
            Optional<ApprovalRegister> result = approvalRegisterRepository.findByDocumentId(nonExistentId);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Тесты метода deleteByDocumentId")
    class DeleteByDocumentIdTests {

        @Test
        @DisplayName("Должен удалить запись из реестра по ID документа")
        void shouldDeleteRegisterByDocumentId() {
            // Given
            Long documentId = document1.getId();
            assertThat(approvalRegisterRepository.existsByDocumentId(documentId)).isTrue();

            // When
            approvalRegisterRepository.deleteByDocumentId(documentId);
            entityManager.flush();
            entityManager.clear(); // Очищаем кэш

            // Then
            boolean exists = approvalRegisterRepository.existsByDocumentId(documentId);
            Optional<ApprovalRegister> deleted = approvalRegisterRepository.findByDocumentId(documentId);

            assertAll("Проверка удаления",
                    () -> assertThat(exists).isFalse(),
                    () -> assertThat(deleted).isEmpty()
            );
        }
    }
}
