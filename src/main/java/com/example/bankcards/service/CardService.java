package com.example.bankcards.service;

import com.example.bankcards.entity.*;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CryptoUtil;
import com.example.bankcards.util.DateUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Service
public class CardService {
    private final CardRepository repo;
    private final UserService userService;
    private final CryptoUtil crypto;

    public CardService(CardRepository repo, UserService userService, CryptoUtil crypto) {
        this.repo = repo;
        this.userService = userService;
        this.crypto = crypto;
    }

    /** Создание карты: валидации, шифрование номера, сохранение last4. */
    @Transactional
    public Card create(String number16, String expiryMmYy, String ownerUsername, BigDecimal initialBalance) {
        validateCardNumber(number16);
        validateExpiry(expiryMmYy);
        if (initialBalance == null || initialBalance.compareTo(BigDecimal.ZERO) < 0)
            throw new BadRequestException("initialBalance must be >= 0");

        User owner = userService.byUsername(ownerUsername);
        Card c = new Card();
        c.setEncNumber(crypto.encrypt(number16));
        c.setLast4(number16.substring(number16.length()-4));
        c.setOwner(owner);
        c.setExpiry(expiryMmYy);
        c.setStatus(CardStatus.ACTIVE);
        c.setBalance(initialBalance);
        c.setCreatedAt(LocalDateTime.now());
        return repo.save(c);
    }
    /** Получение своей карты по id. */
    public Card getOwned(Long cardId, String username) {
        User u = userService.byUsername(username);
        return repo.findByIdAndOwnerId(cardId, u.getId())
                .orElseThrow(() -> new NotFoundException("Card not found"));
    }

    /** Список карт пользователя с фильтрами. */
    public Page<Card> listOwned(String username, CardStatus status, String last4, int page, int size) {
        User u = userService.byUsername(username);
        Pageable p = PageRequest.of(safePage(page), safeSize(size));
        if (status != null) return repo.findByOwnerAndStatus(u, status, p);
        if (last4 != null && !last4.isBlank()) return repo.findByOwnerAndLast4Containing(u, last4, p);
        return repo.findByOwner(u, p);
    }

    /** Список всех карт (для администратора). */
    public Page<Card> listAll(int page, int size) {
        return repo.findAll(PageRequest.of(safePage(page), safeSize(size)));
    }

    @Transactional
    public Card block(Long id) {
        Card c = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("card not found"));
        c.setStatus(CardStatus.BLOCKED);
        repo.save(c);
        // перечитываем уже с подгруженным owner
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("card not found"));
    }

    @Transactional
    public Card activate(Long id) {
        Card c = repo.findById(id)
                .orElseThrow(() -> new NotFoundException("card not found"));
        c.setStatus(CardStatus.ACTIVE);
        repo.save(c);
        return repo.findById(id)
                .orElseThrow(() -> new NotFoundException("card not found"));
    }

    @Transactional
    public void delete(Long id) { repo.deleteById(id); }

    /** Вычисляет «эффективный» статус: если срок истёк — EXPIRED. */
    public CardStatus effectiveStatus(Card c) {
        return DateUtil.isExpired(c.getExpiry()) ? CardStatus.EXPIRED : c.getStatus();
    }

    /** Маска номера по last4: **** **** **** 1234. */
    public String masked(Card c) { return "**** **** **** " + c.getLast4(); }

    private int safePage(int p) { return p < 0 ? 0 : p; }
    private int safeSize(int s) { return (s < 1 || s > 100) ? 10 : s; }

    private void validateCardNumber(String number) {
        if (number == null || !number.matches("\\d{16}"))
            throw new BadRequestException("card number must be 16 digits");
    }
    private void validateExpiry(String expiry) {
        if (expiry == null || !expiry.matches("(0[1-9]|1[0-2])\\/\\d{2}"))
            throw new BadRequestException("expiry must be MM/yy");
    }
    public Card findByIdOr404(Long id) {
        return repo.findById(id).orElseThrow(() -> new NotFoundException("Card not found"));
    }
}
