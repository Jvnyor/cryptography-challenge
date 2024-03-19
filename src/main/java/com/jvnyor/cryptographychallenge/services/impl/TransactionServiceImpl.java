package com.jvnyor.cryptographychallenge.services.impl;

import com.jvnyor.cryptographychallenge.dtos.TransactionRequest;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponse;
import com.jvnyor.cryptographychallenge.entities.Transaction;
import com.jvnyor.cryptographychallenge.repositories.TransactionRepository;
import com.jvnyor.cryptographychallenge.services.TransactionService;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionNotFoundException;
import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    private final AES256TextEncryptor textEncryptor;

    public TransactionServiceImpl(TransactionRepository transactionRepository, AES256TextEncryptor textEncryptor) {
        this.transactionRepository = transactionRepository;
        this.textEncryptor = textEncryptor;
    }

    @Override
    public TransactionResponse createTransaction(TransactionRequest transactionRequest) {
        var transaction = transactionRepository.save(
                new Transaction(
                        getEncryptedValue(transactionRequest.userDocument()),
                        getEncryptedValue(transactionRequest.creditCardToken()),
                        transactionRequest.value()
                )
        );
        return getTransactionResponse(transaction);
    }

    @Override
    public TransactionResponse updateTransaction(long id, TransactionRequest transactionRequest) {
        var transaction = getById(id);
        transaction.setUserDocument(getEncryptedValue(transactionRequest.userDocument()));
        transaction.setCreditCardToken(getEncryptedValue(transactionRequest.creditCardToken()));
        transaction.setValue(transactionRequest.value());
        return getTransactionResponse(transactionRepository.save(transaction));
    }

    @Override
    public void deleteTransaction(long id) {
        transactionRepository.delete(getById(id));
    }

    @Transactional(readOnly = true)
    @Override
    public TransactionResponse getTransaction(long id) {
        return getTransactionResponse(getById(id));
    }

    private Transaction getById(long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction with id %d not found".formatted(id)));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TransactionResponse> getTransactions(Pageable pageable) {
        var transactionsPage = transactionRepository.findAll(pageable);
        if (transactionsPage.hasContent()) {
            return transactionsPage.map(this::getTransactionResponse);
        }
        return Page.empty();
    }

    private String getEncryptedValue(String value) {
        return textEncryptor.encrypt(value);
    }

    private String getDecryptedValue(String value) {
        return textEncryptor.decrypt(value);
    }

    private TransactionResponse getTransactionResponse(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                getDecryptedValue(transaction.getUserDocument()),
                getDecryptedValue(transaction.getCreditCardToken()),
                transaction.getValue()
        );
    }

}
