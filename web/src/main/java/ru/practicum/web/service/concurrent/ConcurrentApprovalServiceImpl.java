package ru.practicum.web.service.concurrent;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.dto.batchOperation.BatchOperationRequest;
import ru.practicum.web.dto.batchOperation.BatchOperationResult;
import ru.practicum.web.dto.concurrent.ConcurrentAttemptDetail;
import ru.practicum.web.dto.concurrent.ConcurrentRequest;
import ru.practicum.web.dto.concurrent.ConcurrentResult;
import ru.practicum.web.exception.DocumentNotFoundException;
import ru.practicum.web.model.ApprovalRegister;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.enums.DocumentStatus;
import ru.practicum.web.model.enums.ResultStatus;
import ru.practicum.web.repository.ApprovalRegisterRepository;
import ru.practicum.web.repository.DocumentRepository;
import ru.practicum.web.service.document.DocumentService;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConcurrentApprovalServiceImpl implements ConcurrentApprovalService{

    private final DocumentService documentService;
    private final DocumentRepository documentRepository;
    private final ApprovalRegisterRepository registerRepository;

    // Запускает тест конкурентного утверждения документа
    @Override
    @Transactional
    public ConcurrentResult runConcurrentApproval(ConcurrentRequest request) {
        log.info("Starting concurrent approval test for document {} with {} threads and {} attempts each",
                request.getDocumentId(), request.getThreads(), request.getAttempts());

        LocalDateTime startTime = LocalDateTime.now();
        long startNanos = System.nanoTime();

        // Проверяем существование документа
        Document document = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found with id: " + request.getDocumentId()));

        // Сбрасываем статус документа в SUBMITTED для теста
        resetDocumentForTest(document);

        // Счетчики результатов
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger registrationErrorCount = new AtomicInteger(0);

        // Детали попыток
        List<ConcurrentAttemptDetail> attemptDetails = Collections.synchronizedList(new ArrayList<>());

        // Executor для параллельного выполнения
        ExecutorService executorService = Executors.newFixedThreadPool(request.getThreads());
        CountDownLatch latch = new CountDownLatch(request.getThreads() * request.getAttempts());

        // Запускаем потоки
        for (int threadId = 0; threadId < request.getThreads(); threadId++) {
            final int currentThreadId = threadId;
            executorService.submit(() -> {
                for (int attemptNum = 0; attemptNum < request.getAttempts(); attemptNum++) {
                    try {
                        long attemptStart = System.nanoTime();

                        // Выполняем попытку утверждения
                        BatchOperationResult result = executeApprovalAttempt(
                                request.getDocumentId(),
                                request.getInitiator() + "-thread" + currentThreadId,
                                currentThreadId,
                                attemptNum
                        );

                        long attemptDuration = (System.nanoTime() - attemptStart) / 1_000_000; // ms

                        // Обновляем счетчики
                        switch (result.getStatus()) {
                            case SUCCESS:
                                successCount.incrementAndGet();
                                break;
                            case CONFLICT:
                                conflictCount.incrementAndGet();
                                break;
                            case REGISTRATION_ERROR:
                                registrationErrorCount.incrementAndGet();
                                break;
                            default:
                                errorCount.incrementAndGet();
                        }

                        // Сохраняем детали
                        attemptDetails.add(ConcurrentAttemptDetail.builder()
                                .threadId(currentThreadId)
                                .attemptNumber(attemptNum)
                                .threadName(Thread.currentThread().getName())
                                .result(result.getStatus())
                                .message(result.getMessage())
                                .attemptTime(LocalDateTime.now())
                                .durationMs(attemptDuration)
                                .build());

                    } catch (Exception e) {
                        log.error("Error in thread {} attempt {}: {}",
                                currentThreadId, attemptNum, e.getMessage());
                        errorCount.incrementAndGet();
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        // Ожидаем завершения всех потоков
        try {
            boolean completed = latch.await(2, TimeUnit.MINUTES);
            if (!completed) {
                log.warn("Test timed out after 2 minutes");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Test interrupted", e);
        } finally {
            executorService.shutdown();
        }

        // Получаем финальный статус документа
        Document finalDocument = documentRepository.findById(request.getDocumentId())
                .orElseThrow(() -> new DocumentNotFoundException(
                        "Document not found after test: " + request.getDocumentId()));

        // Получаем запись в реестре, если есть
        Optional<ApprovalRegister> registerEntry = registerRepository.findByDocumentId(request.getDocumentId());

        long executionTimeMs = (System.nanoTime() - startNanos) / 1_000_000;

        // Формируем детализацию результатов
        Map<String, Integer> resultDetails = new HashMap<>();
        resultDetails.put("SUCCESS", successCount.get());
        resultDetails.put("CONFLICT", conflictCount.get());
        resultDetails.put("REGISTRATION_ERROR", registrationErrorCount.get());
        resultDetails.put("ERROR", errorCount.get());

        // Проверяем ожидаемое поведение
        boolean testPassed = successCount.get() == 1 && finalDocument.getStatus() == DocumentStatus.APPROVED;
        String message = testPassed ?
                "Test passed: one statement was successful" :
                String.format("Test failed: successful attempts %d, final status %s",
                        successCount.get(), finalDocument.getStatus());

        ConcurrentResult result = ConcurrentResult.builder()
                .documentId(request.getDocumentId())
                .documentNumber(finalDocument.getDocumentNumber())
                .finalStatus(finalDocument.getStatus())
                .successfulAttempts(successCount.get())
                .conflictAttempts(conflictCount.get() + registrationErrorCount.get())
                .errorAttempts(errorCount.get())
                .totalAttempts(request.getThreads() * request.getAttempts())
                .resultDetails(resultDetails)
                .registrationNumber(registerEntry.map(ApprovalRegister::getRegistrationNumber).orElse(null))
                .executionTimeMs(executionTimeMs)
                .testStartTime(startTime)
                .testEndTime(LocalDateTime.now())
                .testPassed(testPassed)
                .message(message)
                .build();

        log.info("Concurrent test completed: {}", result);

        // Логируем детали для анализа
        log.debug("Attempt details: {}", attemptDetails);

        return result;
    }

    // Сбрасывает документ в статус SUBMITTED для теста
    @Override
    @Transactional
    public void resetDocumentForTest(Document document) {
        if (document.getStatus() == DocumentStatus.APPROVED) {
            // Если документ уже утвержден, удаляем запись из реестра
            registerRepository.deleteByDocumentId(document.getId());

            // Возвращаем в статус SUBMITTED
            document.setStatus(DocumentStatus.SUBMITTED);
            documentRepository.save(document);

            log.info("Reset document {} from APPROVED to SUBMITTED for test", document.getId());
        } else if (document.getStatus() != DocumentStatus.SUBMITTED) {
            // Если документ не в SUBMITTED, переводим его
            document.setStatus(DocumentStatus.SUBMITTED);
            documentRepository.save(document);
            log.info("Set document {} to SUBMITTED for test", document.getId());
        }
    }

//    // Запускает тест с дополнительными параметрами
//    @Override
//    @Transactional
//    public ConcurrentResult concurrentApprovalSimple(Long documentId, int threads, int attempts,
//                                                     boolean resetDocument, String initiator) {
//        if (resetDocument) {
//            Document document = documentRepository.findById(documentId)
//                    .orElseThrow(() -> new DocumentNotFoundException(
//                            "Document not found: " + documentId));
//            resetDocumentForTest(document);
//        }
//
//        ConcurrentRequest request = new ConcurrentRequest();
//        request.setDocumentId(documentId);
//        request.setThreads(threads);
//        request.setAttempts(attempts);
//        request.setInitiator(initiator);
//
//        return runConcurrentApproval(request);
//    }

    // Проверяет, что после теста в реестре ровно одна запись
    @Override
    @Transactional(readOnly = true)
    public boolean verifySingleRegisterEntry(Long documentId) {
        long count = registerRepository.findByDocumentId(documentId)
                .map(reg -> 1L)
                .orElse(0L);
        return count == 1;
    }

    // Выполняет одну попытку утверждения
    private BatchOperationResult executeApprovalAttempt(Long documentId, String initiator,
                                                        int threadId, int attemptNum) {
        try {
            BatchOperationRequest request = new BatchOperationRequest();
            request.setDocumentIds(List.of(documentId));
            request.setInitiator(initiator);
            request.setComment(String.format("Concurrent test - thread %d, attempt %d",
                    threadId, attemptNum));

            List<BatchOperationResult> results = documentService.approveDocuments(request);

            if (results.isEmpty()) {
                return BatchOperationResult.builder()
                        .documentId(documentId)
                        .status(ResultStatus.REGISTRATION_ERROR)
                        .message("Empty result from approveDocuments")
                        .build();
            }

            return results.get(0);

        } catch (Exception e) {
            log.error("Exception in approval attempt: {}", e.getMessage());
            return BatchOperationResult.builder()
                    .documentId(documentId)
                    .status(ResultStatus.REGISTRATION_ERROR)
                    .message("Exception: " + e.getMessage())
                    .build();
        }
    }
}

