package ru.practicum.web.dto.batchOperation;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class BatchOperationRequest {

    @NotEmpty(message = "Document IDs list cannot be empty")
    @Size(max = 1000, message = "Cannot process more than 1000 documents at once")
    private List<Long> documentIds;

    @NotEmpty(message = "Initiator is required")
    private String initiator;

    private String comment;
}
