package com.jvnyor.cryptographychallenge.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransactionCreateDTO(@NotNull @NotBlank String userDocument, @NotNull @NotBlank String creditCardToken, @Positive double value) {
}
