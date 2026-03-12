package ru.practicum.web.dto.batchOperation;

import lombok.Builder;
import lombok.Data;
import ru.practicum.web.model.enums.ResultStatus;

@Data
@Builder
public class BatchOperationResult {

        private Long documentId;
        private ResultStatus status;
        private String message;
        private String registrationNumber;
}
