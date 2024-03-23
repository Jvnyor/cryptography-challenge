package com.jvnyor.cryptographychallenge.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jvnyor.cryptographychallenge.controllers.exceptions.dtos.ErrorResponseDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionCreateDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponseDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionUpdateDTO;
import com.jvnyor.cryptographychallenge.services.TransactionService;
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

    private TransactionCreateDTO transactionCreateDTO;

    private TransactionUpdateDTO transactionUpdateDTO;

    private TransactionResponseDTO transactionResponseDTO;

    @BeforeEach
    void setUp() {
        this.transactionCreateDTO = new TransactionCreateDTO(
                "userDocument",
                "creditCardToken",
                1
        );
        this.transactionUpdateDTO = new TransactionUpdateDTO(
                "userDocument",
                "creditCardToken",
                1D
        );
        this.transactionResponseDTO = new TransactionResponseDTO(
                1,
                "userDocument",
                "creditCardToken",
                1
        );
    }

    @Test
    void givenTransactionRequest_whenCreateTransaction_thenReturnTransactionResponse() throws Exception {
        when(transactionService.createTransaction(any(TransactionCreateDTO.class))).thenReturn(transactionResponseDTO);

        var result = mockMvc.perform(
                post(URL_TEMPLATE)
                        .content(objectMapper.writeValueAsString(transactionCreateDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(header().string("Location", containsString("/v1/transactions/1")));
        result.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(transactionResponseDTO)));
    }

    @MethodSource("provideParametersForCreateTest")
    @ParameterizedTest
    void givenTransactionRequestWithInvalidValues_whenCreateTransaction_thenExceptionIsThrown(String userDocument, String creditCardToken, double value) throws Exception {

        var result = mockMvc.perform(
                post(URL_TEMPLATE)
                        .content(objectMapper.writeValueAsString(new TransactionCreateDTO(userDocument, creditCardToken, value)))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.message").exists());
        result.andExpect(jsonPath("$.path").value(URL_TEMPLATE));
        result.andExpect(jsonPath("$.exceptionName").value(MethodArgumentNotValidException.class.getSimpleName()));
        result.andExpect(jsonPath("$.status").value(400));
        result.andExpect(jsonPath("$.timestamp").exists());
    }

    static Stream<Arguments> provideParametersForCreateTest() {
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
    void givenTransactionRequest_whenCreateTransaction_butDatabaseRejectsOperation_thenExceptionIsThrown() throws Exception {
        var databaseException = new RuntimeException("Database error");

        when(transactionService.createTransaction(any(TransactionCreateDTO.class))).thenThrow(databaseException);

        var result = mockMvc.perform(
                post(URL_TEMPLATE)
                        .content(objectMapper.writeValueAsString(transactionCreateDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(databaseException, URL_TEMPLATE, 500);

        result.andExpect(status().isInternalServerError());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void givenExistingIdAndTransactionRequest_whenUpdateTransaction_thenReturnTransactionResponse() throws Exception {
        when(transactionService.updateTransaction(anyLong(), any(TransactionUpdateDTO.class))).thenReturn(transactionResponseDTO);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(transactionUpdateDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(MockMvcResultMatchers.content().json(objectMapper.writeValueAsString(transactionResponseDTO)));
    }
    static Stream<Arguments> provideParametersForUpdateTest() {
        final var validCreditCardToken = "validCreditCardToken";
        final var validUserDocument = "validUserDocument";
        final var invalidValue = -1;
        final var emptyString = "";
        return Stream.of(
                Arguments.of(null, null, invalidValue),
                Arguments.of(emptyString, emptyString, invalidValue),
                Arguments.of(validUserDocument, null, invalidValue),
                Arguments.of(null, validCreditCardToken, invalidValue)
        );
    }


    @MethodSource("provideParametersForUpdateTest")
    @ParameterizedTest
    void givenExistingIdAndTransactionRequestWithInvalidValues_whenUpdateTransaction_thenExceptionIsThrown(String userDocument, String creditCardToken, double value) throws Exception {

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(new TransactionUpdateDTO(userDocument, creditCardToken, value)))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.message").value("value: must be greater than 0"));
        result.andExpect(jsonPath("$.path").value(url));
        result.andExpect(jsonPath("$.exceptionName").value(MethodArgumentNotValidException.class.getSimpleName()));
        result.andExpect(jsonPath("$.status").value(400));
        result.andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void givenNonExistentIdAndTransactionRequest_whenUpdateTransaction_thenExceptionIsThrown() throws Exception {
        var transactionNotFoundException = new TransactionNotFoundException(1L);
        when(transactionService.updateTransaction(anyLong(), any(TransactionUpdateDTO.class))).thenThrow(transactionNotFoundException);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(transactionUpdateDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(transactionNotFoundException, url, 404);

        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
        result.andExpect(jsonPath("$.status").value(errorResponseMock.status()));
        result.andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void givenExistingIdAndTransactionRequest_whenUpdateTransaction_butDatabaseRejectsOperation_thenExceptionIsThrown() throws Exception {
        var databaseException = new RuntimeException("Database error");
        when(transactionService.updateTransaction(anyLong(), any(TransactionUpdateDTO.class))).thenThrow(databaseException);

        var url = URL_TEMPLATE + "/1";
        var result = mockMvc.perform(
                put(url)
                        .content(objectMapper.writeValueAsString(transactionUpdateDTO))
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON));

        var errorResponseMock = getErrorResponseMock(databaseException, url, 500);

        result.andExpect(status().isInternalServerError());
        result.andExpect(jsonPath("$.message").value(errorResponseMock.message()));
        result.andExpect(jsonPath("$.path").value(errorResponseMock.path()));
        result.andExpect(jsonPath("$.exceptionName").value(errorResponseMock.exceptionName()));
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