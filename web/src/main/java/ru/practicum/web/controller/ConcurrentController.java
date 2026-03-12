package ru.practicum.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.web.dto.concurrent.ConcurrentRequest;
import ru.practicum.web.dto.concurrent.ConcurrentResult;
import ru.practicum.web.service.concurrent.ConcurrentApprovalService;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/concurrent")
@Tag(name = "Конкурентное утверждение", description = "API для тестирования конкурентного утверждения документов")
public class ConcurrentController {

    private final ConcurrentApprovalService concurrentService;

    @PostMapping("/approval")
    @Operation(summary = "Запустить тест конкурентного утверждения документа",
            description = "Запускает несколько параллельных попыток утвердить один документ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successful"),
            @ApiResponse(responseCode = "409", description = "Conflict"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<ConcurrentResult> runConcurrentApproval(
            @Valid @RequestBody ConcurrentRequest request) {

        log.info("Request to test concurrent approval: {}", request);

        ConcurrentResult result = concurrentService.runConcurrentApproval(request);

        log.info("Test completed: success={}, conflicts={}, finalStatus={}",
                result.getSuccessfulAttempts(), result.getConflictAttempts(), result.getFinalStatus());

        return ResponseEntity.ok(result);
    }

//    @PostMapping("/approval/{documentId}")
//    @Operation(summary = "Запустить тест с параметрами в пути")
//    public ResponseEntity<ConcurrentResult> concurrentApprovalSimple(
//            @PathVariable @Min(1) Long documentId,
//            @RequestParam(defaultValue = "10") int threads,
//            @RequestParam(defaultValue = "5") int attempts,
//            @RequestParam(defaultValue = "test-user") String initiator,
//            @RequestParam(defaultValue = "true") boolean resetDocument) {
//
//        log.info("Request to test concurrent approval for document {} with {} threads and {} attempts",
//                documentId, threads, attempts);
//
//        ConcurrentResult result = concurrentService.concurrentApprovalSimple(
//                documentId, threads, attempts, resetDocument, initiator);
//
//        return ResponseEntity.ok(result);
//    }

    @GetMapping("/approval/{documentId}/status")
    @Operation(summary = "Проверить статус документа после теста")
    public ResponseEntity<Map<String, Object>> checkDocumentStatus(
            @PathVariable Long documentId) {

        log.info("Request to check document status after test: {}", documentId);

        boolean hasSingleRegister = concurrentService.verifySingleRegisterEntry(documentId);

        return ResponseEntity.ok(Map.of(
                "documentId", documentId,
                "hasSingleRegisterEntry", hasSingleRegister,
                "verified", hasSingleRegister ? "One entry in the registry" : "Error in the registry"
        ));
    }
}
