//package ru.practicum.exception;
//
//import jakarta.validation.ConstraintViolationException;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.validation.FieldError;
//import org.springframework.web.ErrorResponse;
//import org.springframework.web.bind.MethodArgumentNotValidException;
//import org.springframework.web.bind.annotation.ExceptionHandler;
//import org.springframework.web.bind.annotation.RestControllerAdvice;
//
//import java.time.LocalDateTime;
//import java.util.stream.Collectors;
//
//@Slf4j
//@RestControllerAdvice
//@RequiredArgsConstructor
//public class GlobalExceptionHandler {
//
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<ErrorResponse> handleValidationExceptions(
//            MethodArgumentNotValidException ex) {
//
//        String errors = ex.getBindingResult()
//                .getFieldErrors()
//                .stream()
//                .map(FieldError::getDefaultMessage)
//                .collect(Collectors.joining(", "));
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.BAD_REQUEST.value())
//                .error("Validation Error")
//                .message(errors)
//                .build();
//
//        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(ConstraintViolationException.class)
//    public ResponseEntity<ErrorResponse> handleConstraintViolation(
//            ConstraintViolationException ex) {
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.BAD_REQUEST.value())
//                .error("Validation Error")
//                .message(ex.getMessage())
//                .build();
//
//        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
//    }
//
//    @ExceptionHandler(Exception.class)
//    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
//        log.error("Unexpected error", ex);
//
//        ErrorResponse errorResponse = ErrorResponse.builder()
//                .timestamp(LocalDateTime.now())
//                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
//                .error("Internal Server Error")
//                .message("Произошла внутренняя ошибка сервера")
//                .build();
//
//        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
//    }
//}
