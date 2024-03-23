package com.jvnyor.cryptographychallenge.repositories;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import static org.junit.jupiter.api.Assertions.assertFalse;

@DataJpaTest
@Sql("/data.sql")
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void givenExistingID_whenDeleteByID_thenDelete() {
        // Given
        long id = 1L;

        // When
        transactionRepository.deleteByID(id);

        // Then
        assertFalse(transactionRepository.existsById(id));
    }
}