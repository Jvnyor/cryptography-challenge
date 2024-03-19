package com.jvnyor.cryptographychallenge.services;

import com.jvnyor.cryptographychallenge.dtos.TransactionRequest;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    TransactionResponse createTransaction(TransactionRequest transactionRequest);
    TransactionResponse updateTransaction(long id, TransactionRequest transactionRequest);
    void deleteTransaction(long id);
    TransactionResponse getTransaction(long id);
    Page<TransactionResponse> getTransactions(Pageable pageable);
}
