package com.jvnyor.cryptographychallenge.repositories;

import com.jvnyor.cryptographychallenge.util.TransactionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
class TransactionRepositoryTest {

    @Autowired
    private TransactionRepository transactionRepository;

    private long existingID;

    @BeforeEach
    void setUp() {
        this.existingID = transactionRepository.save(TransactionFactory.createTransaction()).getId();
    }

    @Test
    void givenExistingID_whenDeleteByID_thenOneRowAffected() {
        int deleteByID = transactionRepository.deleteByID(existingID);

        assertEquals(1, deleteByID);
    }

    @Test
    void givenNonExistentID_whenDeleteByID_thenZeroRowsAffected() {
        long nonExistentID = 100L;

        int deleteByID = transactionRepository.deleteByID(nonExistentID);

        assertEquals(0, deleteByID);
    }

    @AfterEach
    void tearDown() {
        transactionRepository.deleteAll();
    }
}