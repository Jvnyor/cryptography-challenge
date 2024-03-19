package com.jvnyor.cryptographychallenge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvnyor.cryptographychallenge.controllers.exceptions.dtos.ErrorResponse;
import com.jvnyor.cryptographychallenge.dtos.TransactionRequest;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponse;
import com.jvnyor.cryptographychallenge.services.TransactionService;
import com.jvnyor.cryptographychallenge.services.exceptions.TransactionNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    private static final String URL_TEMPLATE = "/v1/transactions";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TransactionService transactionService;

    private TransactionRequest transactionRequest;

    private TransactionResponse transactionResponse;

    @BeforeEach
    void setUp() {
        this.transactionRequest = new TransactionRequest(
                "userDocument",
                "creditCardToken",
                1
        );
        this.transactionResponse = new TransactionResponse(
                1,
                "userDocument",
                "creditCardToken",
                1
        );
    }

    @Test
    void givenTransactionRequest_whenCreateTransaction_thenReturnTransactionResponse() throws Exception {
        when(transactionService.createTransaction(any(TransactionRequest.class))).thenReturn(transactionResponse);

        var result = mockMvc.perform(
                post(URL_TEMPLATE)
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(transactionResponse)));
    }

    @Test
    void givenTransactionRequest_whenCreateTransaction_butDatabaseRejectsOperation_thenExceptionIsThrown() throws Exception {
        var databaseException = new RuntimeException("Database error");

        when(transactionService.createTransaction(any(TransactionRequest.class))).thenThrow(databaseException);

        var result = mockMvc.perform(
                post(URL_TEMPLATE)
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(databaseException, URL_TEMPLATE, 500);

        result.andExpect(status().isInternalServerError());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.className").value(errorResponseMock.className()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void givenExistingIdAndTransactionRequest_whenUpdateTransaction_thenReturnTransactionResponse() throws Exception {
        when(transactionService.updateTransaction(anyLong(), any(TransactionRequest.class))).thenReturn(transactionResponse);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(transactionResponse)));
    }

    @Test
    void givenNonExistentIdAndTransactionRequest_whenUpdateTransaction_thenExceptionIsThrown() throws Exception {
        var transactionNotFoundException = new TransactionNotFoundException("Transaction with id 1 not found");
        when(transactionService.updateTransaction(anyLong(), any(TransactionRequest.class))).thenThrow(transactionNotFoundException);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(transactionNotFoundException, url, 404);

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.className").value(errorResponseMock.className()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void givenExistingIdAndTransactionRequest_whenUpdateTransaction_butDatabaseRejectsOperation_thenExceptionIsThrown() throws Exception {
        var databaseException = new RuntimeException("Database error");
        when(transactionService.updateTransaction(anyLong(), any(TransactionRequest.class))).thenThrow(databaseException);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(transactionRequest))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(databaseException, url, 500);

        result.andExpect(status().isInternalServerError());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.className").value(errorResponseMock.className()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());
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
    }

    @Test
    void givenNonExistentId_whenDeleteTransaction_thenExceptionIsThrown() throws Exception {
        var transactionNotFoundException = new TransactionNotFoundException("Transaction with id 1 not found");
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
        result.andExpect(jsonPath("$.className").value(errorResponseMock.className()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());
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
        result.andExpect(jsonPath("$.className").value(errorResponseMock.className()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getTransaction() {
    }

    @Test
    void getTransactions() {
    }

    private ErrorResponse getErrorResponseMock(Exception exception, String path, int status) {
        return new ErrorResponse(
                exception.getMessage(),
                path,
                exception.getClass().getSimpleName(),
                status,
                LocalDateTime.now()
        );
    }
}