package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Юнит-тесты CardService:
 * - create: валидации, шифрование, last4, сохранение
 * - getOwned / listOwned: выборка "своих" карт и NotFound
 * - listAll: пагинация
 * - block / activate: изменение статуса + повторное чтение
 * - effectiveStatus / masked: вычисление статуса и маски
 */
@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock private CardRepository repo;
    @Mock private UserService userService;
    @Mock private CryptoUtil crypto;

    @InjectMocks
    private CardService service;

    private User owner;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(10L);
        owner.setUsername("bob");
        // Остальные поля для юзера сервису не нужны
    }

    @Test
    void create_ok_encrypts_and_saves() {
        String plain = "1111222233334444";
        String enc   = "IV:ENC";
        when(userService.byUsername("bob")).thenReturn(owner);
        when(crypto.encrypt(plain)).thenReturn(enc);

        // save возвращает ту же карту с выставленным id
        when(repo.save(any(Card.class))).thenAnswer(inv -> {
            Card c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });

        Card c = service.create(plain, "12/29", "bob", BigDecimal.valueOf(100));

        assertThat(c.getId()).isEqualTo(1L);
        assertThat(c.getEncNumber()).isEqualTo(enc);
        assertThat(c.getLast4()).isEqualTo("4444");
        assertThat(c.getOwner()).isSameAs(owner);
        assertThat(c.getExpiry()).isEqualTo("12/29");
        assertThat(c.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(c.getBalance()).isEqualByComparingTo("100");
        assertThat(c.getCreatedAt()).isInstanceOf(LocalDateTime.class);

        verify(repo).save(any(Card.class));
        verifyNoMoreInteractions(repo);
    }

    @Test
    void create_fails_when_number_invalid() {
        assertThatThrownBy(() ->
                service.create("123", "12/29", "bob", BigDecimal.TEN)
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("card number must be 16 digits");
    }

    @Test
    void create_fails_when_expiry_invalid() {
        assertThatThrownBy(() ->
                service.create("1111222233334444", "13/29", "bob", BigDecimal.TEN)
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expiry must be MM/yy");
    }

    @Test
    void create_fails_when_initialBalance_negative() {
        assertThatThrownBy(() ->
                service.create("1111222233334444", "12/29", "bob", BigDecimal.valueOf(-1))
        ).isInstanceOf(BadRequestException.class)
                .hasMessageContaining("initialBalance must be >= 0");
    }

    @Test
    void getOwned_ok() {
        when(userService.byUsername("bob")).thenReturn(owner);
        Card card = card(100L, owner, "12/29", CardStatus.ACTIVE, "4444");
        when(repo.findByIdAndOwnerId(100L, 10L)).thenReturn(Optional.of(card));

        Card found = service.getOwned(100L, "bob");
        assertThat(found).isSameAs(card);
    }

    @Test
    void getOwned_notFound() {
        when(userService.byUsername("bob")).thenReturn(owner);
        when(repo.findByIdAndOwnerId(100L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwned(100L, "bob"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    void listOwned_default_all_user_cards() {
        when(userService.byUsername("bob")).thenReturn(owner);
        Card c1 = card(1L, owner, "12/29", CardStatus.ACTIVE, "1111");
        Page<Card> page = new PageImpl<>(List.of(c1));

        when(repo.findByOwner(eq(owner), any(Pageable.class))).thenReturn(page);

        Page<Card> res = service.listOwned("bob", null, null, 0, 10);
        assertThat(res.getContent()).containsExactly(c1);
        verify(repo).findByOwner(eq(owner), any(Pageable.class));
    }

    @Test
    void listOwned_with_status_filter() {
        when(userService.byUsername("bob")).thenReturn(owner);
        Card c1 = card(1L, owner, "12/29", CardStatus.BLOCKED, "1111");
        Page<Card> page = new PageImpl<>(List.of(c1));

        when(repo.findByOwnerAndStatus(eq(owner), eq(CardStatus.BLOCKED), any(Pageable.class)))
                .thenReturn(page);

        Page<Card> res = service.listOwned("bob", CardStatus.BLOCKED, null, 0, 10);
        assertThat(res.getContent()).containsExactly(c1);
    }

    @Test
    void listOwned_with_last4_filter() {
        when(userService.byUsername("bob")).thenReturn(owner);
        Card c1 = card(1L, owner, "12/29", CardStatus.ACTIVE, "1234");
        Page<Card> page = new PageImpl<>(List.of(c1));

        when(repo.findByOwnerAndLast4Containing(eq(owner), eq("1234"), any(Pageable.class)))
                .thenReturn(page);

        Page<Card> res = service.listOwned("bob", null, "1234", 0, 10);
        assertThat(res.getContent()).containsExactly(c1);
    }

    @Test
    void listAll_returns_page() {
        Card c1 = card(1L, owner, "12/29", CardStatus.ACTIVE, "1111");
        when(repo.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(c1)));

        Page<Card> res = service.listAll(0, 10);
        assertThat(res.getContent()).containsExactly(c1);
    }

    @Test
    void block_ok_changes_status_and_reloads() {
        Card before = card(5L, owner, "12/29", CardStatus.ACTIVE, "2222");
        Card after  = card(5L, owner, "12/29", CardStatus.BLOCKED, "2222");

        // findById (первый вызов) -> ACTIVE
        when(repo.findById(5L)).thenReturn(Optional.of(before))  // 1-й вызов
                .thenReturn(Optional.of(after));  // 2-й вызов (после save)
        when(repo.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card res = service.block(5L);

        assertThat(res.getStatus()).isEqualTo(CardStatus.BLOCKED);
        verify(repo, times(2)).findById(5L);
        verify(repo).save(argThat(c -> c.getStatus() == CardStatus.BLOCKED));
    }

    @Test
    void block_notFound() {
        when(repo.findById(999L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.block(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("card not found");
    }

    @Test
    void activate_ok_changes_status_and_reloads() {
        Card before = card(6L, owner, "12/29", CardStatus.BLOCKED, "3333");
        Card after  = card(6L, owner, "12/29", CardStatus.ACTIVE,  "3333");

        when(repo.findById(6L)).thenReturn(Optional.of(before))
                .thenReturn(Optional.of(after));
        when(repo.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));

        Card res = service.activate(6L);

        assertThat(res.getStatus()).isEqualTo(CardStatus.ACTIVE);
        verify(repo, times(2)).findById(6L);
        verify(repo).save(argThat(c -> c.getStatus() == CardStatus.ACTIVE));
    }

    @Test
    void delete_calls_repository() {
        service.delete(123L);
        verify(repo).deleteById(123L);
    }

    @Test
    void effectiveStatus_returns_expired_when_date_passed() {
        Card expired = card(7L, owner, "01/20", CardStatus.ACTIVE, "4444"); // заведомо в прошлом
        assertThat(service.effectiveStatus(expired)).isEqualTo(CardStatus.EXPIRED);
    }

    @Test
    void effectiveStatus_keeps_actual_when_not_expired() {
        Card active = card(8L, owner, "12/99", CardStatus.BLOCKED, "5555");
        assertThat(service.effectiveStatus(active)).isEqualTo(CardStatus.BLOCKED);
    }

    @Test
    void masked_returns_last4_mask() {
        Card c = card(9L, owner, "12/29", CardStatus.ACTIVE, "9876");
        assertThat(service.masked(c)).isEqualTo("**** **** **** 9876");
    }

    @Test
    void create_propagates_crypto_exception() {
        when(userService.byUsername("bob")).thenReturn(owner);
        when(crypto.encrypt("1111222233334444"))
                .thenThrow(new IllegalStateException("Encrypt failed"));

        assertThatThrownBy(() ->
                service.create("1111222233334444", "12/29", "bob", BigDecimal.TEN)
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Encrypt failed");
    }

    // --- helpers ---
    private Card card(Long id, User owner, String expiry, CardStatus status, String last4) {
        Card c = new Card();
        c.setId(id);
        c.setOwner(owner);
        c.setExpiry(expiry);
        c.setStatus(status);
        c.setLast4(last4);
        c.setCreatedAt(LocalDateTime.now());
        return c;
    }
}
