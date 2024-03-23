package com.jvnyor.cryptographychallenge.dtos;

public record TransactionResponseDTO(long id, String userDocument, String creditCardToken, double value) {
}
