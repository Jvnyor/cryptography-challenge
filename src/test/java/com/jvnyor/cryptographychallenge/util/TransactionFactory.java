package com.jvnyor.cryptographychallenge.util;

import com.jvnyor.cryptographychallenge.entities.Transaction;

public class TransactionFactory {
    private TransactionFactory() {
    }

    public static Transaction createTransaction() {
        return new Transaction(null, "12345678901", "1234567890123456", 1000.0);
    }
}
