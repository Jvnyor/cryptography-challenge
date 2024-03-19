package com.jvnyor.cryptographychallenge.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record TransactionRequest(@NotNull @NotEmpty String userDocument, @NotNull @NotEmpty String creditCardToken, @Min(value = 0) double value) {
}
