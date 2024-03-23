package com.jvnyor.cryptographychallenge.controllers.exceptions.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        String message,
        String path,
        String exceptionName,
        int status,
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        LocalDateTime timestamp
) {
}
