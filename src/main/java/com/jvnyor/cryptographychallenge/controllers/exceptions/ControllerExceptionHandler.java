package com.jvnyor.cryptographychallenge.controllers.exceptions;

import com.jvnyor.cryptographychallenge.controllers.exceptions.dtos.ErrorResponseDTO;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionDeletionException;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.stream.Collectors;

@ControllerAdvice
public class ControllerExceptionHandler {

    @ExceptionHandler(Exception.class)
    private ResponseEntity<Object> handleException(Exception exception, HttpServletRequest request) {

        final var internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(internalServerError)
                .body(new ErrorResponseDTO(
                        exception.getMessage(),
                        request.getRequestURI(),
                        exception.getClass().getSimpleName(),
                        internalServerError.value(),
                        LocalDateTime.now())
                );
    }

    @ExceptionHandler({NoResourceFoundException.class, TransactionNotFoundException.class})
    private ResponseEntity<Object> handleNotFoundExceptions(Exception exception, HttpServletRequest request) {

        final var notFound = HttpStatus.NOT_FOUND;
        return ResponseEntity
                .status(notFound)
                .body(new ErrorResponseDTO(
                        exception.getMessage(),
                        request.getRequestURI(),
                        exception.getClass().getSimpleName(),
                        notFound.value(),
                        LocalDateTime.now())
                );
    }

    @ExceptionHandler(TransactionDeletionException.class)
    private ResponseEntity<Object> handleTransactionDeletionException(TransactionDeletionException exception, HttpServletRequest request) {

        final var internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(internalServerError)
                .body(new ErrorResponseDTO(
                        exception.getMessage(),
                        request.getRequestURI(),
                        exception.getClass().getSimpleName(),
                        internalServerError.value(),
                        LocalDateTime.now())
                );
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConstraintViolationException.class, HttpMessageNotReadableException.class})
    private ResponseEntity<Object> handleValidationExceptions(Exception exception, HttpServletRequest request) {

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponseDTO(
                        exception.getMessage(),
                        request.getRequestURI(),
                        exception.getClass().getSimpleName(),
                        HttpStatus.BAD_REQUEST.value(),
                        LocalDateTime.now())
                );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    private ResponseEntity<Object> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception, HttpServletRequest request) {

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponseDTO(
                        getMethodArgumentNotValidExceptionMessage(exception),
                        request.getRequestURI(),
                        exception.getClass().getSimpleName(),
                        HttpStatus.BAD_REQUEST.value(),
                        LocalDateTime.now())
                );
    }

    @NonNull
    private String getMethodArgumentNotValidExceptionMessage(@NonNull MethodArgumentNotValidException exception) {
        return Optional.of(exception.getBindingResult().getFieldErrors())
                .filter(errors -> !errors.isEmpty())
                .map(errors -> errors.stream()
                        .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                        .collect(Collectors.joining(", ")))
                .orElseGet(exception::getMessage);
    }
}
