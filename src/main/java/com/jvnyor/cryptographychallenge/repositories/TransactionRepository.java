package com.jvnyor.cryptographychallenge.repositories;

import com.jvnyor.cryptographychallenge.entities.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Transactional
    @Modifying
    @Query("delete from Transaction t where t.id = ?1")
    int deleteByID(long id);
}
