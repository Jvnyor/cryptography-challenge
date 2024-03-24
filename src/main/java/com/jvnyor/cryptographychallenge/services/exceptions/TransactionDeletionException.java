package com.jvnyor.cryptographychallenge.services.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class TransactionDeletionException extends RuntimeException {
    public TransactionDeletionException(long id) {
        super("Failed to delete transaction with id: " + id);
    }
}