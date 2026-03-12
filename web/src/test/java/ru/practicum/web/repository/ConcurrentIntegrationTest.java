/*
package ru.practicum.web.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.http.MediaType;
import ru.practicum.web.dto.concurrent.ConcurrentRequest;
import ru.practicum.web.dto.concurrent.ConcurrentResult;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.enums.DocumentStatus;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DataJpaTest
@DisplayName("Интеграционные тесты ConcurrentApproval")
@AutoConfigureTestDatabase(replace= AutoConfigureTestDatabase.Replace.NONE)
class ConcurrentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private ApprovalRegisterRepository registerRepository;

    private Document testDocument;

    @BeforeEach
    void setUp() {
        testDocument = new Document();
        testDocument.setDocumentNumber("TEST-CONCURRENT-001");
        testDocument.setAuthor("Тестовый автор");
        testDocument.setTitle("Тест конкурентности");
        testDocument.setStatus(DocumentStatus.SUBMITTED);
        testDocument = documentRepository.save(testDocument);
    }

    @Test
    @DisplayName("Должен вернуть ровно одно успешное утверждение")
    void shouldReturnExactlyOneSuccess() throws Exception {
        // Given
        ConcurrentRequest request = new ConcurrentRequest();
        request.setDocumentId(testDocument.getId());
        request.setThreads(5);
        request.setAttempts(3);
        request.setInitiator("integration-test");

        // When/Then
        mockMvc.perform(post("/api/test/concurrent/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(testDocument.getId()))
                .andExpect(jsonPath("$.successfulAttempts").value(1))
                .andExpect(jsonPath("$.finalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.testPassed").value(true));
    }

    @Test
    @DisplayName("Должен вернуть 404 для несуществующего документа")
    void shouldReturn404ForNonExistentDocument() throws Exception {
        // Given
        ConcurrentRequest request = new ConcurrentRequest();
        request.setDocumentId(99999L);
        request.setThreads(2);
        request.setAttempts(1);

        // When/Then
        mockMvc.perform(post("/api/test/concurrent/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Должен проверить статус документа")
    void shouldCheckDocumentStatus() throws Exception {
        // First run the test
        ConcurrentRequest request = new ConcurrentRequest();
        request.setDocumentId(testDocument.getId());
        request.setThreads(3);
        request.setAttempts(2);
        request.setInitiator("test");

        String testResult = mockMvc.perform(post("/api/test/concurrent/approval")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ConcurrentResult result = objectMapper.readValue(testResult, ConcurrentResult.class);

        // Then check status
        mockMvc.perform(get("/api/test/concurrent/approval/{id}/status", testDocument.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(testDocument.getId()))
                .andExpect(jsonPath("$.hasSingleRegisterEntry").value(true));
    }
}
*/
