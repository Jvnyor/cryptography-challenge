package com.jvnyor.cryptographychallenge.controllers;

import com.jvnyor.cryptographychallenge.dtos.TransactionRequest;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponse;
import com.jvnyor.cryptographychallenge.services.TransactionService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequestMapping("/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(@RequestBody @Valid TransactionRequest transactionRequest) {
        var transaction = transactionService.createTransaction(transactionRequest);
        return ResponseEntity
                .created(ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id})")
                        .buildAndExpand(transaction.id())
                        .toUri())
                .body(transaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable Long id, @RequestBody @Valid TransactionRequest transactionRequest) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, transactionRequest));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponse>> getTransactions(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactions(pageable));
    }
}
