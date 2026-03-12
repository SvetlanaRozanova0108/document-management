package ru.practicum.web.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.practicum.web.model.ApprovalRegister;

import java.util.Optional;

@Repository
public interface ApprovalRegisterRepository extends JpaRepository<ApprovalRegister, Long> {

    boolean existsByDocumentId(Long documentId);

    Optional<ApprovalRegister> findByDocumentId(Long documentId);

    @Modifying
    @Query("DELETE FROM ApprovalRegister ar WHERE ar.document.id = :documentId")
    void deleteByDocumentId(@Param("documentId") Long documentId);
}
