package com.jvnyor.cryptographychallenge.controllers;

import com.jvnyor.cryptographychallenge.dtos.TransactionRequestDTO;
import com.jvnyor.cryptographychallenge.dtos.TransactionResponseDTO;
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
    public ResponseEntity<TransactionResponseDTO> createTransaction(@RequestBody @Valid TransactionRequestDTO transactionRequestDTO) {
        var transaction = transactionService.createTransaction(transactionRequestDTO);
        return ResponseEntity
                .created(ServletUriComponentsBuilder
                        .fromCurrentRequest()
                        .path("/{id})")
                        .buildAndExpand(transaction.id())
                        .toUri())
                .body(transaction);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> updateTransaction(@PathVariable Long id, @RequestBody @Valid TransactionRequestDTO transactionRequestDTO) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, transactionRequestDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransaction(@PathVariable Long id) {
        transactionService.deleteTransaction(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> getTransaction(@PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransaction(id));
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponseDTO>> getTransactions(@ParameterObject Pageable pageable) {
        return ResponseEntity.ok(transactionService.getTransactions(pageable));
    }
}
