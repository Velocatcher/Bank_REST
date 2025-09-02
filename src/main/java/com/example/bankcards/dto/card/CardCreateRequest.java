package com.example.bankcards.dto.card;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;

public record CardCreateRequest(
        @Pattern(regexp = "\\d{16}", message = "card number must be 16 digits") String number,
        @Pattern(regexp = "(0[1-9]|1[0-2])\\/\\d{2}", message = "expiry must be MM/yy") String expiry,
        @DecimalMin(value = "0.00", message = "initialBalance must be >= 0") BigDecimal initialBalance,
        @NotBlank String ownerUsername
) { }
