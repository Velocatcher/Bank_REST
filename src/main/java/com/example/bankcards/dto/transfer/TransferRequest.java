package com.example.bankcards.dto.transfer;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull Long fromCardId,
        @NotNull Long toCardId,
        @DecimalMin("0.01")  BigDecimal amount
        ) {}
