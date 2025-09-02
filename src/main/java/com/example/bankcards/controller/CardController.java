package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.service.CardService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cards")
public class CardController { // <-- имя класса должно совпадать с именем файла

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    @Transactional
    public CardResponse create(@RequestBody @Valid CardCreateRequest req) {
        Card c = cardService.create(req.number(), req.expiry(), req.ownerUsername(), req.initialBalance());
        return toDto(c);
    }
    @Transactional(readOnly = true)
    @GetMapping
    public Page<CardResponse> list(
            @AuthenticationPrincipal UserDetails ud,
            @RequestParam(name = "status", required = false) CardStatus status,
            @RequestParam(name = "last4", required = false) String last4,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size
    ) {
        if (page < 0) throw new BadRequestException("page must be >= 0");
        if (size < 1) throw new BadRequestException("size must be >= 1");

        boolean admin = isAdmin(ud);
        Page<Card> src = admin
                ? cardService.listAll(page, size) // при желании можно добавить фильтры и для админа
                : cardService.listOwned(ud.getUsername(), status, last4, page, size);

        return src.map(this::toDto);
    }
    @Transactional(readOnly = true)
    @GetMapping("/{id}")
    public CardResponse get(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        boolean admin = isAdmin(ud);
        Card c = admin
                ? cardService.findByIdOr404(id)
                : cardService.getOwned(id, ud.getUsername());
        return toDto(c);
    }

    @PatchMapping("/{id}/block")
    @Transactional
    public CardResponse block(@PathVariable Long id, @AuthenticationPrincipal UserDetails ud) {
        // Пользователь может блокировать только свою карту; админ — любую.
        if (!isAdmin(ud)) cardService.getOwned(id, ud.getUsername());
        return toDto(cardService.block(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/activate")
    @Transactional
    public CardResponse activate(@PathVariable Long id) {
        return toDto(cardService.activate(id));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        cardService.delete(id);
    }

    private CardResponse toDto(Card c) {
        return new CardResponse(
                c.getId(),
                cardService.masked(c),
                c.getExpiry(),
                cardService.effectiveStatus(c),
                c.getBalance(),
                c.getOwner().getUsername()
        );
    }

    private boolean isAdmin(UserDetails ud) {
        return ud.getAuthorities().stream().anyMatch(a -> {
            String v = a.getAuthority();
            return "ROLE_ADMIN".equals(v);
        });
    }
}