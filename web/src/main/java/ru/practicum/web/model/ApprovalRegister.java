package ru.practicum.web.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@Entity
@Table(name = "approval_register")
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApprovalRegister {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    Document document;

    @Column(name = "approved_by", nullable = false)
    String approvedBy;

    @Column(name = "approved_at", nullable = false)
    LocalDateTime approvedAt;

    @Column(name = "registration_number", nullable = false, unique = true)
    String registrationNumber;

    @PrePersist
    public void prePersist() {
        if (registrationNumber == null) {
            registrationNumber = "REG-" + UUID.randomUUID().toString();
        }
        if (approvedAt == null) {
            approvedAt = LocalDateTime.now();
        }
    }
}
