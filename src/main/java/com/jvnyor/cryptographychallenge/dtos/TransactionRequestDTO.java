package com.jvnyor.cryptographychallenge.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransactionRequestDTO(@NotBlank(message = "must not be blank or null") String userDocument, @NotBlank(message = "must not be blank or null") String creditCardToken, @Positive double value) {
}
