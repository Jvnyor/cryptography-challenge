package com.jvnyor.cryptographychallenge.services;

import com.jvnyor.cryptographychallenge.dtos.TransactionRequest;
import com.jvnyor.cryptographychallenge.entities.Transaction;
import com.jvnyor.cryptographychallenge.repositories.TransactionRepository;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionNotFoundException;
import com.jvnyor.cryptographychallenge.services.impl.TransactionServiceImpl;
import org.jasypt.util.text.AES256TextEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String DECRYPTED_MESSAGE = "decrypted";

    public static final String ENCRYPTED_MESSAGE = "encrypted";

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AES256TextEncryptor textEncryptor;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private TransactionRequest transactionRequest;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        this.transactionRequest = new TransactionRequest(
                "userDocument",
                "creditCardToken",
                1
        );
        this.transaction = new Transaction(
                1L,
                ENCRYPTED_MESSAGE,
                ENCRYPTED_MESSAGE,
                1
        );
    }

    @Test
    void givenTransactionRequest_whenCreateTransaction_thenReturnTransactionResponse() {
        when(textEncryptor.encrypt(any(String.class))).thenReturn(ENCRYPTED_MESSAGE);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(textEncryptor.decrypt(any(String.class))).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse = transactionService.createTransaction(transactionRequest);

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse.value())
        );

        verify(textEncryptor, times(2)).encrypt(any(String.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(textEncryptor, times(2)).decrypt(any(String.class));
    }

    @Test
    void givenDatabaseRejection_whenCreateTransaction_thenExceptionIsThrown() {
        when(textEncryptor.encrypt(any(String.class))).thenReturn(ENCRYPTED_MESSAGE);
        when(transactionRepository.save(any(Transaction.class))).thenThrow(DataIntegrityViolationException.class);

        assertThrows(DataIntegrityViolationException.class, () -> transactionService.createTransaction(transactionRequest));
        verify(textEncryptor, times(2)).encrypt(any(String.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void givenExistingIdAndTransactionRequest_whenUpdateTransaction_thenReturnTransactionResponse() {
        when(transactionRepository.findById(any(Long.class))).thenReturn(Optional.of(transaction));
        when(textEncryptor.encrypt(any(String.class))).thenReturn(ENCRYPTED_MESSAGE);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(textEncryptor.decrypt(any(String.class))).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse = transactionService.updateTransaction(1L, transactionRequest);

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse.value())
        );

        verify(transactionRepository, times(1)).findById(1L);
        verify(textEncryptor, times(2)).encrypt(any(String.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(textEncryptor, times(2)).decrypt(any(String.class));
    }

    @Test
    void givenNonExistentIdAndTransactionRequest_whenUpdateTransaction_thenExceptionIsThrown() {
        when(transactionRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        var transactionNotFoundException = assertThrows(TransactionNotFoundException.class, () -> transactionService.updateTransaction(1L, transactionRequest));
        assertEquals("Transaction with id 1 not found", transactionNotFoundException.getMessage());

        verify(transactionRepository, times(1)).findById(1L);
        verify(textEncryptor, times(0)).encrypt(any(String.class));
        verify(transactionRepository, times(0)).save(any(Transaction.class));
        verify(textEncryptor, times(0)).decrypt(any(String.class));
    }

    @Test
    void givenExistingId_whenDeleteTransaction_thenNoExceptionIsThrown() {
        when(transactionRepository.findById(any(Long.class))).thenReturn(Optional.of(transaction));
        doNothing().when(transactionRepository).delete(any(Transaction.class));

        assertDoesNotThrow(() -> transactionService.deleteTransaction(1L));

        verify(transactionRepository, times(1)).findById(1L);
        verify(transactionRepository, times(1)).delete(any(Transaction.class));
    }

    @Test
    void givenNonExistentId_whenDeleteTransaction_thenExceptionIsThrown() {
        when(transactionRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        var transactionNotFoundException = assertThrows(TransactionNotFoundException.class, () -> transactionService.deleteTransaction(1L));
        assertEquals("Transaction with id 1 not found", transactionNotFoundException.getMessage());

        verify(transactionRepository, times(1)).findById(1L);
        verify(transactionRepository, times(0)).delete(any(Transaction.class));
    }


    @Test
    void givenExistingId_whenGetTransaction_thenReturnTransactionResponse() {
        when(transactionRepository.findById(any(Long.class))).thenReturn(Optional.of(transaction));
        when(textEncryptor.decrypt(any(String.class))).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse = transactionService.getTransaction(1L);

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse.value())
        );

        verify(transactionRepository, times(1)).findById(1L);
        verify(textEncryptor, times(2)).decrypt(any(String.class));
    }

    @Test
    void givenNonExistentId_whenGetTransaction_thenExceptionIsThrown() {
        when(transactionRepository.findById(any(Long.class))).thenReturn(Optional.empty());

        var transactionNotFoundException = assertThrows(TransactionNotFoundException.class, () -> transactionService.updateTransaction(1L, transactionRequest));
        assertEquals("Transaction with id 1 not found", transactionNotFoundException.getMessage());

        verify(transactionRepository, times(1)).findById(1L);
        verify(textEncryptor, times(0)).decrypt(any(String.class));
    }

    @Test
    void givenParameterObjectPageable_whenGetTransactions_thenReturnTransactionsPaginated() {
        when(transactionRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.singletonList(transaction)));
        when(textEncryptor.decrypt(any(String.class))).thenReturn(DECRYPTED_MESSAGE);

        var transactions = transactionService.getTransactions(PageRequest.of(0, 20));
        var transactionResponse = transactions.getContent().get(0);

        assertAll("Return transactions with decrypted fields",
                () -> assertFalse(transactions.isEmpty()),
                () -> assertEquals(transaction.getId(), transactionResponse.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse.value())
        );

        verify(transactionRepository, times(1)).findAll(any(PageRequest.class));
        verify(textEncryptor, times(2)).decrypt(any(String.class));
    }

    @Test
    void givenParameterObjectPageable_whenGetTransactions_butDatabaseIsEmpty_thenReturnEmptyPage() {
        when(transactionRepository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(Collections.emptyList()));

        var transactions = transactionService.getTransactions(PageRequest.of(0, 20));

        assertTrue(transactions.isEmpty());

        verify(transactionRepository, times(1)).findAll(any(PageRequest.class));
        verify(textEncryptor, times(0)).decrypt(any(String.class));
    }
}