package com.jvnyor.cryptographychallenge.services;

import com.jvnyor.cryptographychallenge.dtos.TransactionRequestDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    TransactionResponseDTO createTransaction(TransactionRequestDTO transactionRequestDTO);
    TransactionResponseDTO updateTransaction(long id, TransactionRequestDTO transactionRequestDTO);
    void deleteTransaction(long id);
    TransactionResponseDTO getTransaction(long id);
    Page<TransactionResponseDTO> getTransactions(Pageable pageable);
}
