package com.jvnyor.cryptographychallenge.services.impl;

import com.jvnyor.cryptographychallenge.dtos.TransactionCreateDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponseDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionUpdateDTO;
import com.jvnyor.cryptographychallenge.entities.Transaction;
import com.jvnyor.cryptographychallenge.repositories.TransactionRepository;
import com.jvnyor.cryptographychallenge.services.TransactionService;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionDeletionException;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionNotFoundException;
import org.jasypt.util.text.AES256TextEncryptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

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
    public TransactionResponseDTO createTransaction(TransactionCreateDTO transactionCreateDTO) {
        var transaction = transactionRepository.save(
                new Transaction(
                        null,
                        textEncryptor.encrypt(transactionCreateDTO.userDocument()),
                        textEncryptor.encrypt(transactionCreateDTO.creditCardToken()),
                        transactionCreateDTO.value()
                )
        );
        return getTransactionResponse(transaction);
    }

    @Override
    public TransactionResponseDTO updateTransaction(long id, TransactionUpdateDTO transactionUpdateDTO) {
        var existingTransaction = findById(id);
        return getTransactionResponse(
                updateTransactionFieldsIfChanged(transactionUpdateDTO, existingTransaction)
                        .map(transactionRepository::save)
                        .orElse(existingTransaction)
        );
    }

    private Optional<Transaction> updateTransactionFieldsIfChanged(TransactionUpdateDTO transactionUpdateDTO, Transaction transaction) {
        var updated = false;

        var userDocument = transactionUpdateDTO.userDocument();
        if (userDocument != null && (!userDocument.equals(textEncryptor.decrypt(transaction.getUserDocument())))) {
            transaction.setUserDocument(textEncryptor.encrypt(userDocument));
            updated = true;
        }

        var creditCardToken = transactionUpdateDTO.creditCardToken();
        if (creditCardToken != null && (!creditCardToken.equals(textEncryptor.decrypt(transaction.getCreditCardToken())))) {
            transaction.setCreditCardToken(textEncryptor.encrypt(creditCardToken));
            updated = true;
        }

        var value = transactionUpdateDTO.value();
        if (value != null && value != transaction.getValue()) {
            transaction.setValue(value);
            updated = true;
        }

        return updated ? Optional.of(transaction) : Optional.empty();
    }

    @Override
    public void deleteTransaction(long id) {
        int deleteByID = transactionRepository.deleteByID(existsById(id));
        if (deleteByID == 0) {
            throw new TransactionDeletionException(id);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public TransactionResponseDTO getTransaction(long id) {
        return getTransactionResponse(findById(id));
    }

    private Transaction findById(long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    private long existsById(long id) {
        return Optional.of(id)
                .filter(transactionRepository::existsById)
                .orElseThrow(() -> new TransactionNotFoundException(id));
    }

    @Transactional(readOnly = true)
    @Override
    public Page<TransactionResponseDTO> getTransactions(Pageable pageable) {
        return Optional.of(transactionRepository.findAll(pageable))
                .filter(Page::hasContent)
                .map(page -> page.map(this::getTransactionResponse))
                .orElse(Page.empty());
    }

    private TransactionResponseDTO getTransactionResponse(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                textEncryptor.decrypt(transaction.getUserDocument()),
                textEncryptor.decrypt(transaction.getCreditCardToken()),
                transaction.getValue()
        );
    }

}
