package com.jvnyor.cryptographychallenge.dtos;

import java.io.Serial;
import java.io.Serializable;

public record TransactionResponseDTO(
        long id,
        String userDocument,
        String creditCardToken,
        double value
) implements Serializable {

    @Serial
    private static final long serialVersionUID = -484298548931886889L;
}
