package com.jvnyor.cryptographychallenge.services;

import com.jvnyor.cryptographychallenge.dtos.TransactionCreateDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionUpdateDTO;
import com.jvnyor.cryptographychallenge.entities.Transaction;
import com.jvnyor.cryptographychallenge.repositories.TransactionRepository;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionNotFoundException;
import com.jvnyor.cryptographychallenge.services.impl.TransactionServiceImpl;
import org.jasypt.util.text.AES256TextEncryptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    private static final String DECRYPTED_MESSAGE = "decrypted";

    public static final String ENCRYPTED_MESSAGE_NOT_UPDATED = "encrypted";

    public static final String ENCRYPTED_MESSAGE_UPDATED = ENCRYPTED_MESSAGE_NOT_UPDATED + "1";

    private static final String TRANSACTION_WITH_ID_1_NOT_FOUND = "Transaction with id 1 not found";

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AES256TextEncryptor textEncryptor;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    private TransactionCreateDTO transactionCreateDTO;

    private TransactionUpdateDTO transactionUpdateDTO;

    private Transaction transaction;

    @BeforeEach
    void setUp() {
        this.transactionCreateDTO = new TransactionCreateDTO(
                "userDocument",
                "creditCardToken",
                1
        );
        this.transactionUpdateDTO = new TransactionUpdateDTO(
                "userDocument1",
                "creditCardToken1",
                1D
        );
        this.transaction = new Transaction(
                1L,
                ENCRYPTED_MESSAGE_NOT_UPDATED,
                ENCRYPTED_MESSAGE_NOT_UPDATED,
                1
        );
    }

    @Test
    void givenTransactionCreateDTO_whenCreateTransaction_thenReturnTransactionResponse() {
        when(textEncryptor.encrypt(anyString())).thenReturn(ENCRYPTED_MESSAGE_NOT_UPDATED);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(textEncryptor.decrypt(anyString())).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse = transactionService.createTransaction(transactionCreateDTO);

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse.value())
        );

        verify(textEncryptor, times(2)).encrypt(anyString());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(textEncryptor, times(2)).decrypt(anyString());
    }

    @Test
    void givenDatabaseRejection_whenCreateTransaction_thenExceptionIsThrown() {
        when(textEncryptor.encrypt(anyString())).thenReturn(ENCRYPTED_MESSAGE_NOT_UPDATED);
        when(transactionRepository.save(any(Transaction.class))).thenThrow(DataIntegrityViolationException.class);

        assertThrows(DataIntegrityViolationException.class, () -> transactionService.createTransaction(transactionCreateDTO));
        verify(textEncryptor, times(2)).encrypt(anyString());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void givenExistingIdAndTransactionUpdateDTO_whenUpdateTransaction_thenReturnTransactionResponse() {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(transaction));
        when(textEncryptor.encrypt(anyString())).thenReturn(ENCRYPTED_MESSAGE_UPDATED);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(textEncryptor.decrypt(anyString())).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse = transactionService.updateTransaction(1L, transactionUpdateDTO);

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse.value())
        );

        verify(transactionRepository, times(1)).findById(anyLong());
        verify(textEncryptor, times(2)).encrypt(anyString());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(textEncryptor, times(4)).decrypt(anyString());
    }

    @Test
    void givenExistingIdAndTransactionUpdateDTOWithNull_whenUpdateTransaction_thenReturnTransactionResponse_butWithoutPersistenceInvocation() {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(transaction));
        when(textEncryptor.decrypt(anyString())).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse = transactionService.updateTransaction(1L, new TransactionUpdateDTO(null, null, null));

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse.value())
        );

        verify(transactionRepository, times(1)).findById(anyLong());
        verify(textEncryptor, times(0)).encrypt(anyString());
        verify(transactionRepository, times(0)).save(any(Transaction.class));
        verify(textEncryptor, times(2)).decrypt(anyString());
    }

    @Test
    void givenExistingIdAndTransactionUpdateDTOWithSameOfExistingTransactionValues_whenUpdateTransaction_thenReturnTransactionResponse_butWithoutPersistenceInvocation() {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(transaction));
        when(textEncryptor.decrypt(anyString())).thenReturn(DECRYPTED_MESSAGE);
        when(textEncryptor.decrypt(anyString())).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse = transactionService.updateTransaction(1L, new TransactionUpdateDTO(DECRYPTED_MESSAGE, DECRYPTED_MESSAGE, transaction.getValue()));

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse.value())
        );

        verify(transactionRepository, times(1)).findById(anyLong());
        verify(textEncryptor, times(0)).encrypt(anyString());
        verify(transactionRepository, times(0)).save(any(Transaction.class));
        verify(textEncryptor, times(4)).decrypt(anyString());
    }

    @MethodSource("provideParametersForTestWithUserDocumentAndCreditCardTokenFieldsValidation")
    @ParameterizedTest
    void givenExistingIdAndTransactionUpdateDTOWithUserDocumentAndCreditCardTokenNullAndUpdatedValue_whenUpdateTransaction_thenReturnTransactionResponse(String userDocument, String creditCardToken) {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(transaction));
        when(textEncryptor.encrypt(anyString())).thenReturn(ENCRYPTED_MESSAGE_UPDATED);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(textEncryptor.decrypt(anyString())).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse1 = transactionService.updateTransaction(1L, new TransactionUpdateDTO(userDocument, creditCardToken, transaction.getValue()));

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse1.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse1.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse1.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse1.value())
        );

        verify(transactionRepository, times(1)).findById(anyLong());
        verify(textEncryptor, times(1)).encrypt(anyString());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(textEncryptor, times(3)).decrypt(anyString());
    }

    @Test
    void givenExistingIdAndTransactionUpdateDTOWithOtherFieldsNullAndValidValueToUpdate_whenUpdateTransaction_thenReturnTransactionResponse() {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(transaction));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(transaction);
        when(textEncryptor.decrypt(anyString())).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse1 = transactionService.updateTransaction(1L, new TransactionUpdateDTO(null, null, 2D));

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse1.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse1.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse1.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse1.value())
        );

        verify(transactionRepository, times(1)).findById(anyLong());
        verify(textEncryptor, times(0)).encrypt(anyString());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(textEncryptor, times(2)).decrypt(anyString());
    }

    static Stream<Arguments> provideParametersForTestWithUserDocumentAndCreditCardTokenFieldsValidation() {
        final var validCreditCardToken = "creditCardToken1";
        final var validUserDocument = "userDocument1";
        final var validValue = 1;
        return Stream.of(
                Arguments.of(null, validCreditCardToken, validValue),
                Arguments.of(validUserDocument, null, validValue)
        );
    }

    @Test
    void givenNonExistentIdAndTransactionUpdateDTO_whenUpdateTransaction_thenExceptionIsThrown() {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.updateTransaction(1L, transactionUpdateDTO), TRANSACTION_WITH_ID_1_NOT_FOUND);

        verify(transactionRepository, times(1)).findById(anyLong());
        verify(textEncryptor, times(0)).encrypt(anyString());
        verify(transactionRepository, times(0)).save(any(Transaction.class));
        verify(textEncryptor, times(0)).decrypt(anyString());
    }

    @Test
    void givenDatabaseRejection_whenUpdateTransaction_thenExceptionIsThrown() {
        when(transactionRepository.findById(any(Long.class))).thenReturn(Optional.of(transaction));
        when(textEncryptor.encrypt(any(String.class))).thenReturn(ENCRYPTED_MESSAGE_UPDATED);
        when(transactionRepository.save(any(Transaction.class))).thenThrow(DataIntegrityViolationException.class);

        assertThrows(DataIntegrityViolationException.class, () -> transactionService.updateTransaction(1L, transactionUpdateDTO));

        verify(transactionRepository, times(1)).findById(anyLong());
        verify(textEncryptor, times(2)).encrypt(any(String.class));
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    void givenExistingId_whenDeleteTransaction_thenNoExceptionIsThrown() {
        when(transactionRepository.existsById(anyLong())).thenReturn(true);
        doNothing().when(transactionRepository).deleteByID(anyLong());

        assertDoesNotThrow(() -> transactionService.deleteTransaction(1L));

        verify(transactionRepository, times(1)).existsById(anyLong());
        verify(transactionRepository, times(1)).deleteByID(anyLong());
    }

    @Test
    void givenNonExistentId_whenDeleteTransaction_thenExceptionIsThrown() {
        when(transactionRepository.existsById(anyLong())).thenReturn(false);

        assertThrows(TransactionNotFoundException.class, () -> transactionService.deleteTransaction(1L), TRANSACTION_WITH_ID_1_NOT_FOUND);

        verify(transactionRepository, times(1)).existsById(anyLong());
        verify(transactionRepository, times(0)).deleteByID(anyLong());
    }

    @Test
    void givenExistingId_whenGetTransaction_thenReturnTransactionResponse() {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.of(transaction));
        when(textEncryptor.decrypt(anyString())).thenReturn(DECRYPTED_MESSAGE);

        var transactionResponse = transactionService.getTransaction(1L);

        assertAll("Return transaction response with decrypted fields",
                () -> assertEquals(transaction.getId(), transactionResponse.id()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.userDocument()),
                () -> assertEquals(DECRYPTED_MESSAGE, transactionResponse.creditCardToken()),
                () -> assertEquals(transaction.getValue(), transactionResponse.value())
        );

        verify(transactionRepository, times(1)).findById(anyLong());
        verify(textEncryptor, times(2)).decrypt(any(String.class));
    }

    @Test
    void givenNonExistentId_whenGetTransaction_thenExceptionIsThrown() {
        when(transactionRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(TransactionNotFoundException.class, () -> transactionService.getTransaction(1L), TRANSACTION_WITH_ID_1_NOT_FOUND);

        verify(transactionRepository, times(1)).findById(anyLong());
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