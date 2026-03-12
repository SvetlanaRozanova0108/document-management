package ru.practicum.web.model;

import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Table;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.*;
import ru.practicum.web.model.enums.DocumentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "documents")
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Document {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "document_number", nullable = false, unique = true)
    String documentNumber;

    @Column(name = "author", nullable = false)
    String author;

    @Column(name = "title", nullable = false)
    String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    DocumentStatus status;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @OneToMany(mappedBy = "document", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<DocumentHistory> history = new ArrayList<>();

    public void addHistory(DocumentHistory historyItem) {
        history.add(historyItem);
        historyItem.setDocument(this);
    }
}
