package com.jvnyor.cryptographychallenge.dtos;

public record TransactionResponse(long id, String userDocument, String creditCardToken, double value) {
}
