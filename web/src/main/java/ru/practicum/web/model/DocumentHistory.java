package ru.practicum.web.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@Entity
@Table(name = "document_history")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DocumentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    Document document;

    @Column(name = "initiator", nullable = false)
    String initiator;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    DocumentAction action;

    @Column(name = "comment")
    String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    public enum DocumentAction {
        SUBMIT,
        APPROVE
    }
}
