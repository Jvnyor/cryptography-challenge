package com.jvnyor.cryptographychallenge.services.impl;

import com.jvnyor.cryptographychallenge.dtos.TransactionRequestDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponseDTO;
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
    public TransactionResponseDTO createTransaction(TransactionRequestDTO transactionRequestDTO) {
        return createDTOFromEntity(transactionRepository.save(createOrUpdateEntityFromDTO(new Transaction(), transactionRequestDTO)));
    }

    @Override
    public TransactionResponseDTO updateTransaction(long id, TransactionRequestDTO transactionRequestDTO) {
        return createDTOFromEntity(transactionRepository.save(createOrUpdateEntityFromDTO(findById(id), transactionRequestDTO)));
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
        return createDTOFromEntity(findById(id));
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
                .map(page -> page.map(this::createDTOFromEntity))
                .orElse(Page.empty());
    }

    private Transaction createOrUpdateEntityFromDTO(Transaction transaction, TransactionRequestDTO transactionUpdateDTO) {
        transaction.setUserDocument(textEncryptor.encrypt(transactionUpdateDTO.userDocument().trim()));
        transaction.setCreditCardToken(textEncryptor.encrypt(transactionUpdateDTO.creditCardToken().trim()));
        transaction.setValue(transactionUpdateDTO.value());
        return transaction;
    }

    private TransactionResponseDTO createDTOFromEntity(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                textEncryptor.decrypt(transaction.getUserDocument()),
                textEncryptor.decrypt(transaction.getCreditCardToken()),
                transaction.getValue()
        );
    }

}
