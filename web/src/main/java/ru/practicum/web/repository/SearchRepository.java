package ru.practicum.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.practicum.web.model.Document;
import ru.practicum.web.model.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchRepository extends JpaRepository<Document, Long> {

    // Поиск по статусу
    List<Document> findByStatus(DocumentStatus status);

    // Поиск по автору (точное совпадение)
    List<Document> findByAuthor(String author);

    // Поиск по автору (частичное совпадение)
    List<Document> findByAuthorContainingIgnoreCase(String author);

    // Поиск по периоду дат создания
    List<Document> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // Поиск по статусу и автору
    List<Document> findByStatusAndAuthor(DocumentStatus status, String author);

    // Поиск по статусу и периоду
    List<Document> findByStatusAndCreatedAtBetween(DocumentStatus status, LocalDateTime start, LocalDateTime end);

    // Поиск по автору и периоду
    List<Document> findByAuthorAndCreatedAtBetween(String author, LocalDateTime start, LocalDateTime end);

    // Поиск по всем трем параметрам
    List<Document> findByStatusAndAuthorAndCreatedAtBetween(
            DocumentStatus status, String author, LocalDateTime start, LocalDateTime end);

    // Статистика
    long countByStatus(DocumentStatus status);
}