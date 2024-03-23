package com.jvnyor.cryptographychallenge.services;

import com.jvnyor.cryptographychallenge.dtos.TransactionCreateDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionUpdateDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TransactionService {
    TransactionResponseDTO createTransaction(TransactionCreateDTO transactionCreateDTO);
    TransactionResponseDTO updateTransaction(long id, TransactionUpdateDTO transactionUpdateDTO);
    void deleteTransaction(long id);
    TransactionResponseDTO getTransaction(long id);
    Page<TransactionResponseDTO> getTransactions(Pageable pageable);
}
