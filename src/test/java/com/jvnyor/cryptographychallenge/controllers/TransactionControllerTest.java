package com.jvnyor.cryptographychallenge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvnyor.cryptographychallenge.controllers.exceptions.dtos.ErrorResponseDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionRequestDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponseDTO;
import com.jvnyor.cryptographychallenge.services.TransactionService;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionDeletionException;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    private static final String URL_TEMPLATE = "/v1/transactions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private TransactionRequestDTO transactionRequestDTO;

    private TransactionResponseDTO transactionResponseDTO;

    @BeforeEach
    void setUp() {
        this.transactionRequestDTO = new TransactionRequestDTO(
                "userDocument",
                "creditCardToken",
                1
        );
        this.transactionResponseDTO = new TransactionResponseDTO(
                1,
                "userDocument",
                "creditCardToken",
                1
        );
    }

    @Test
    void givenTransactionRequestDTO_whenCreateTransaction_thenReturnTransactionResponse() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequestDTO.class))).thenReturn(transactionResponseDTO);

        var result = mockMvc.perform(
                post(URL_TEMPLATE)
                        .content(objectMapper.writeValueAsString(transactionRequestDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(header().string("Location", containsString("/v1/transactions/1")));
        result.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(transactionResponseDTO)));

        verify(transactionService, times(1)).createTransaction(any(TransactionRequestDTO.class));
    }

    @MethodSource("provideParametersForCreateAndUpdateTest")
    @ParameterizedTest
    void givenTransactionRequestDTOWithInvalidValues_whenCreateTransaction_thenExceptionIsThrown(String userDocument, String creditCardToken, double value) throws Exception {

        var result = mockMvc.perform(
                post(URL_TEMPLATE)
                        .content(objectMapper.writeValueAsString(new TransactionRequestDTO(userDocument, creditCardToken, value)))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.message").exists());
        result.andExpect(jsonPath("$.path").value(URL_TEMPLATE));
        result.andExpect(jsonPath("$.exceptionName").value(MethodArgumentNotValidException.class.getSimpleName()));
        result.andExpect(jsonPath("$.status").value(400));
        result.andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService, times(0)).createTransaction(any(TransactionRequestDTO.class));
    }

    static Stream<Arguments> provideParametersForCreateAndUpdateTest() {
        final var validCreditCardToken = "validCreditCardToken";
        final var validUserDocument = "validUserDocument";
        final var validValue = 1;
        final var invalidValue = -1;
        final var emptyString = "";
        return Stream.of(
                Arguments.of(null, validCreditCardToken, validValue),
                Arguments.of(emptyString, validCreditCardToken, validValue),
                Arguments.of(validUserDocument, null, validValue),
                Arguments.of(validUserDocument, emptyString, validValue),
                Arguments.of(validUserDocument, validCreditCardToken, invalidValue)
        );
    }

    @Test
    void givenTransactionRequestDTO_whenCreateTransaction_butDatabaseRejectsOperation_thenExceptionIsThrown() throws Exception {
        var databaseException = new RuntimeException("Database error");

        when(transactionService.createTransaction(any(TransactionRequestDTO.class))).thenThrow(databaseException);

        var result = mockMvc.perform(
                post(URL_TEMPLATE)
                        .content(objectMapper.writeValueAsString(transactionRequestDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(databaseException, URL_TEMPLATE, 500);

        result.andExpect(status().isInternalServerError());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService, times(1)).createTransaction(any(TransactionRequestDTO.class));
    }

    @Test
    void givenExistingIdAndTransactionRequestDTO_whenUpdateTransaction_thenReturnTransactionResponse() throws Exception {
        when(transactionService.updateTransaction(anyLong(), any(TransactionRequestDTO.class))).thenReturn(transactionResponseDTO);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(transactionRequestDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(transactionResponseDTO)));

        verify(transactionService, times(1)).updateTransaction(anyLong(), any(TransactionRequestDTO.class));
    }

    @MethodSource("provideParametersForCreateAndUpdateTest")
    @ParameterizedTest
    void givenExistingIdAndTransactionRequestDTOWithInvalidValues_whenUpdateTransaction_thenExceptionIsThrown(String userDocument, String creditCardToken, double value) throws Exception {

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(new TransactionRequestDTO(userDocument, creditCardToken, value)))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.message").exists());
        result.andExpect(jsonPath("$.path").value(url));
        result.andExpect(jsonPath("$.exceptionName").value(MethodArgumentNotValidException.class.getSimpleName()));
        result.andExpect(jsonPath("$.status").value(400));
        result.andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService, times(0)).updateTransaction(anyLong(), any(TransactionRequestDTO.class));
    }

    @Test
    void givenNonExistentIdAndTransactionRequestDTO_whenUpdateTransaction_thenExceptionIsThrown() throws Exception {
        var transactionNotFoundException = new TransactionNotFoundException(1L);
        when(transactionService.updateTransaction(anyLong(), any(TransactionRequestDTO.class))).thenThrow(transactionNotFoundException);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(transactionRequestDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(transactionNotFoundException, url, 404);

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService, times(1)).updateTransaction(anyLong(), any(TransactionRequestDTO.class));
    }

    @Test
    void givenExistingIdAndTransactionRequestDTO_whenUpdateTransaction_butDatabaseRejectsOperation_thenExceptionIsThrown() throws Exception {
        var databaseException = new RuntimeException("Database error");
        when(transactionService.updateTransaction(anyLong(), any(TransactionRequestDTO.class))).thenThrow(databaseException);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(transactionRequestDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(databaseException, url, 500);

        result.andExpect(status().isInternalServerError());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService, times(1)).updateTransaction(anyLong(), any(TransactionRequestDTO.class));
    }

    @Test
    void givenExistingId_whenDeleteTransaction_thenNoExceptionIsThrown() throws Exception {
        doNothing().when(transactionService).deleteTransaction(anyLong());

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                delete(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNoContent());

        verify(transactionService, times(1)).deleteTransaction(anyLong());
    }

    @Test
    void givenNonExistentId_whenDeleteTransaction_thenExceptionIsThrown() throws Exception {
        var transactionNotFoundException = new TransactionNotFoundException(1L);
        doThrow(transactionNotFoundException).when(transactionService).deleteTransaction(anyLong());

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                delete(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(transactionNotFoundException, url, 404);

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService, times(1)).deleteTransaction(anyLong());
    }

    @Test
    void givenExistingId_whenDeleteTransaction_butZeroRowsAffected_thenExceptionIsThrown() throws Exception {
        var transactionDeletionException = new TransactionDeletionException(1L);
        doThrow(transactionDeletionException).when(transactionService).deleteTransaction(anyLong());

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                delete(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(transactionDeletionException, url, 500);

        result.andExpect(status().isInternalServerError());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService, times(1)).deleteTransaction(anyLong());
    }

    @Test
    void givenExistingId_whenDeleteTransaction_butDatabaseRejectsOperation_thenExceptionIsThrown() throws Exception {
        var databaseException = new RuntimeException("Database error");
        doThrow(databaseException).when(transactionService).deleteTransaction(anyLong());

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                delete(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(databaseException, url, 500);

        result.andExpect(status().isInternalServerError());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService, times(1)).deleteTransaction(anyLong());
    }

    @Test
    void givenExistingId_whenGetTransaction_thenReturnTransactionResponse() throws Exception {
        when(transactionService.getTransaction(anyLong())).thenReturn(transactionResponseDTO);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                get(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(transactionResponseDTO)));

        verify(transactionService, times(1)).getTransaction(anyLong());
    }

    @Test
    void givenNonExistentId_whenGetTransaction_thenExceptionIsThrown() throws Exception {
        var transactionNotFoundException = new TransactionNotFoundException(1L);
        when(transactionService.getTransaction(anyLong())).thenThrow(transactionNotFoundException);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                get(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(transactionNotFoundException, url, 404);

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());

        verify(transactionService, times(1)).getTransaction(anyLong());
    }

    @Test
    void givenParameterObjectPageableRequest_whenGetTransactions_thenReturnPageOfTransactionResponse() throws Exception {
        var transactionResponsePage = new PageImpl<>(Collections.singletonList(transactionResponseDTO));
        when(transactionService.getTransactions(any(PageRequest.class))).thenReturn(transactionResponsePage);

        var url = URL_TEMPLATE + "?page=0&size=20";
        var result = mockMvc.perform(
                get(url)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(transactionResponsePage)));

        verify(transactionService, times(1)).getTransactions(any(PageRequest.class));
    }

    @Test
    void givenParameterObjectPageableRequestWithoutParams_whenGetTransactions_thenReturnPageOfTransactionResponse() throws Exception {
        var transactionResponsePage = new PageImpl<>(Collections.singletonList(transactionResponseDTO));
        when(transactionService.getTransactions(any(PageRequest.class))).thenReturn(transactionResponsePage);

        var result = mockMvc.perform(
                get(URL_TEMPLATE)
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(transactionResponsePage)));

        verify(transactionService, times(1)).getTransactions(any(PageRequest.class));
    }

    private ErrorResponseDTO getErrorResponseMock(Exception exception, String path, int status) {
        return new ErrorResponseDTO(
                exception.getMessage(),
                path,
                exception.getClass().getSimpleName(),
                status,
                LocalDateTime.now()
        );
    }
}