package com.jvnyor.cryptographychallenge.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.io.Serial;
import java.io.Serializable;

public record TransactionRequestDTO(
        @NotBlank(message = "must not be blank or null") String userDocument,
        @NotBlank(message = "must not be blank or null") String creditCardToken,
        @Positive double value
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 5288366271365974156L;
}
