package com.example.bankcards.dto.card;

import com.example.bankcards.entity.CardStatus;

import java.math.BigDecimal;

public record CardResponse(
        Long id,
        String maskedNumber,
        String expiry, CardStatus status,
        BigDecimal balance,
        String owner) {}
