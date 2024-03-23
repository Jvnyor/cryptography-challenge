package com.jvnyor.cryptographychallenge.dtos;

import jakarta.validation.constraints.Positive;

public record TransactionUpdateDTO(String userDocument, String creditCardToken, @Positive Double value) {
    public TransactionUpdateDTO {
        if (userDocument() != null && userDocument().isBlank()) {
            userDocument = null;
        }
        if (userDocument() != null && creditCardToken().isBlank()) {
            creditCardToken = null;
        }
    }
}
