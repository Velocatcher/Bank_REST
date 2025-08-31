package com.example.bankcards.service;

import com.example.bankcards.entity.*;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransferService {
    private final TransferRepository transferRepo;
    private final CardRepository cardRepo;
    private final UserService userService;
    private final CardService cardService;

    public TransferService(TransferRepository transferRepo, CardRepository cardRepo, UserService userService, CardService cardService) {
        this.transferRepo = transferRepo; this.cardRepo = cardRepo; this.userService = userService; this.cardService = cardService;
    }

    /**
     * Перевод между картами одного пользователя.
     * Валидации: разные карты, обе принадлежат пользователю, статусы ACTIVE и не EXPIRED, сумма > 0, достаточно средств.
     */
    @Transactional
    public Long transfer(String username, Long fromCardId, Long toCardId, BigDecimal amount) {
        if (fromCardId == null || toCardId == null) throw new BadRequestException("card ids required");
        if (fromCardId.equals(toCardId)) throw new BadRequestException("from and to must differ");
        if (amount == null || amount.compareTo(new BigDecimal("0.01")) < 0)
            throw new BadRequestException("amount must be >= 0.01");

        var user = userService.byUsername(username);
        Card from = cardRepo.findByIdAndOwnerId(fromCardId, user.getId())
                .orElseThrow(() -> new ForbiddenException("not your source card"));
        Card to = cardRepo.findByIdAndOwnerId(toCardId, user.getId())
                .orElseThrow(() -> new ForbiddenException("not your target card"));

        if (cardService.effectiveStatus(from) != CardStatus.ACTIVE || cardService.effectiveStatus(to) != CardStatus.ACTIVE)
            throw new BadRequestException("cards must be ACTIVE");
        if (from.getBalance().compareTo(amount) < 0)
            throw new BadRequestException("insufficient funds");

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        Transfer t = Transfer.builder()
                .fromCard(from).toCard(to).user(user)
                .amount(amount).createdAt(LocalDateTime.now())
                .build();
        transferRepo.save(t);
        return t.getId();
    }
}
