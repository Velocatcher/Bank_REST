package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Transfer;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Тесты под фактический TransferService:
 * - userService.byUsername(username)
 * - cardRepo.findByIdAndOwnerId(cardId, userId) (дважды)
 * - cardService.effectiveStatus(card) (1–2 раза)
 * - transferRepo.save(transfer)
 * Код сервиса: см. TransferService. Репозиторий: TransferRepository.
 */
@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock TransferRepository transferRepo;
    @Mock CardRepository cardRepo;
    @Mock UserService userService;
    @Mock CardService cardService;

    @InjectMocks TransferService transferService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        setId(user, 10L);
        user.setUsername("alice");
    }

    // ---------- УСПЕШНЫЙ ПЕРЕВОД ----------
    @Test
    void transfer_success() {
        Long fromId = 1L, toId = 2L;
        BigDecimal amount = new BigDecimal("250.00");

        Card from = new Card();
        from.setBalance(new BigDecimal("1000.00"));
        Card to = new Card();
        to.setBalance(new BigDecimal("100.00"));

        when(userService.byUsername("alice")).thenReturn(user);
        when(cardRepo.findByIdAndOwnerId(fromId, user.getId())).thenReturn(Optional.of(from));
        when(cardRepo.findByIdAndOwnerId(toId, user.getId())).thenReturn(Optional.of(to));
        when(cardService.effectiveStatus(from)).thenReturn(CardStatus.ACTIVE);
        when(cardService.effectiveStatus(to)).thenReturn(CardStatus.ACTIVE);

        // Эмулируем присвоение ID тем же объектам, что переданы в save(...)
        doAnswer(inv -> {
            Transfer t = inv.getArgument(0, Transfer.class);
            setId(t, 99L);
            return t;
        }).when(transferRepo).save(any(Transfer.class));

        Long id = transferService.transfer("alice", fromId, toId, amount);

        // Балансы изменены
        assertEquals(0, from.getBalance().compareTo(new BigDecimal("750.00")));
        assertEquals(0, to.getBalance().compareTo(new BigDecimal("350.00")));
        // Вернулся ID, выставленный "JPA"
        assertEquals(99L, id);

        // ------ Точные verify под реальные вызовы ------
        verify(userService, times(1)).byUsername(eq("alice"));
        verify(cardRepo, times(1)).findByIdAndOwnerId(eq(fromId), eq(user.getId()));
        verify(cardRepo, times(1)).findByIdAndOwnerId(eq(toId), eq(user.getId()));
        verify(cardService, times(1)).effectiveStatus(same(from));
        verify(cardService, times(1)).effectiveStatus(same(to));
        verify(transferRepo, times(1)).save(argThat(t ->
                t.getFromCard() == from &&
                        t.getToCard() == to &&
                        t.getUser() == user &&
                        amount.compareTo(t.getAmount()) == 0
        ));
        verifyNoMoreInteractions(userService, cardRepo, cardService, transferRepo);
    }

    // ---------- НЕКОРРЕКТНЫЕ ВХОДНЫЕ ДАННЫЕ ----------
    @Test
    void transfer_nullIds() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> transferService.transfer("alice", null, 2L, new BigDecimal("10")));
        assertEquals("card ids required", ex.getMessage());
        verifyNoInteractions(userService, cardRepo, cardService, transferRepo);
    }

    @Test
    void transfer_sameCards() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> transferService.transfer("alice", 1L, 1L, new BigDecimal("10")));
        assertEquals("from and to must differ", ex.getMessage());
        verifyNoInteractions(userService, cardRepo, cardService, transferRepo);
    }

    @Test
    void transfer_amountTooSmall_orNull() {
        // amount < 0.01
        BadRequestException ex1 = assertThrows(BadRequestException.class,
                () -> transferService.transfer("alice", 1L, 2L, new BigDecimal("0.001")));
        assertEquals("amount must be >= 0.01", ex1.getMessage());

        // amount == null
        BadRequestException ex2 = assertThrows(BadRequestException.class,
                () -> transferService.transfer("alice", 1L, 2L, null));
        assertEquals("amount must be >= 0.01", ex2.getMessage());

        verifyNoInteractions(userService, cardRepo, cardService, transferRepo);
    }

    // ---------- ДОСТУП / ПРИНАДЛЕЖНОСТЬ ----------
    @Test
    void transfer_fromNotOwned_forbidden() {
        when(userService.byUsername("alice")).thenReturn(user);
        when(cardRepo.findByIdAndOwnerId(1L, user.getId())).thenReturn(Optional.empty());

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> transferService.transfer("alice", 1L, 2L, new BigDecimal("10")));
        assertEquals("not your source card", ex.getMessage());

        verify(userService, times(1)).byUsername(eq("alice"));
        verify(cardRepo, times(1)).findByIdAndOwnerId(eq(1L), eq(user.getId()));
        verify(cardRepo, never()).findByIdAndOwnerId(eq(2L), anyLong());
        verifyNoInteractions(cardService, transferRepo);
    }

    @Test
    void transfer_toNotOwned_forbidden() {
        Card from = new Card(); from.setBalance(new BigDecimal("50"));
        when(userService.byUsername("alice")).thenReturn(user);
        when(cardRepo.findByIdAndOwnerId(1L, user.getId())).thenReturn(Optional.of(from));
        when(cardRepo.findByIdAndOwnerId(2L, user.getId())).thenReturn(Optional.empty());

        ForbiddenException ex = assertThrows(ForbiddenException.class,
                () -> transferService.transfer("alice", 1L, 2L, new BigDecimal("10")));
        assertEquals("not your target card", ex.getMessage());

        verify(userService, times(1)).byUsername(eq("alice"));
        verify(cardRepo, times(1)).findByIdAndOwnerId(eq(1L), eq(user.getId()));
        verify(cardRepo, times(1)).findByIdAndOwnerId(eq(2L), eq(user.getId()));
        verifyNoInteractions(cardService, transferRepo);
    }

    // ---------- СТАТУС КАРТ ----------
    @Test
    void transfer_cardsMustBeActive_sourceBlocked() {
        Card from = new Card(); from.setBalance(new BigDecimal("100"));
        Card to = new Card();   to.setBalance(new BigDecimal("100"));

        when(userService.byUsername("alice")).thenReturn(user);
        when(cardRepo.findByIdAndOwnerId(1L, user.getId())).thenReturn(Optional.of(from));
        when(cardRepo.findByIdAndOwnerId(2L, user.getId())).thenReturn(Optional.of(to));
        when(cardService.effectiveStatus(from)).thenReturn(CardStatus.BLOCKED); // уже не ACTIVE

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> transferService.transfer("alice", 1L, 2L, new BigDecimal("10")));
        assertEquals("cards must be ACTIVE", ex.getMessage());

        verify(userService, times(1)).byUsername(eq("alice"));
        verify(cardRepo, times(1)).findByIdAndOwnerId(eq(1L), eq(user.getId()));
        verify(cardRepo, times(1)).findByIdAndOwnerId(eq(2L), eq(user.getId()));
        verify(cardService, times(1)).effectiveStatus(same(from));
        // из-за short-circuit второй effectiveStatus(to) не вызывается
        verify(cardService, never()).effectiveStatus(same(to));
        verifyNoInteractions(transferRepo);
    }

    // ---------- БАЛАНС ----------
    @Test
    void transfer_insufficientFunds() {
        Card from = new Card(); from.setBalance(new BigDecimal("5"));
        Card to = new Card();   to.setBalance(new BigDecimal("0"));

        when(userService.byUsername("alice")).thenReturn(user);
        when(cardRepo.findByIdAndOwnerId(1L, user.getId())).thenReturn(Optional.of(from));
        when(cardRepo.findByIdAndOwnerId(2L, user.getId())).thenReturn(Optional.of(to));
        when(cardService.effectiveStatus(from)).thenReturn(CardStatus.ACTIVE);
        when(cardService.effectiveStatus(to)).thenReturn(CardStatus.ACTIVE);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> transferService.transfer("alice", 1L, 2L, new BigDecimal("10")));
        assertEquals("insufficient funds", ex.getMessage());

        // статусы проверены для обеих карт (оба ACTIVE, поэтому оба вызова состоялись)
        verify(cardService, times(1)).effectiveStatus(same(from));
        verify(cardService, times(1)).effectiveStatus(same(to));
        verifyNoInteractions(transferRepo);
    }

    // ---------- утилита рефлексии для установки id ----------
    private static void setId(Object target, Long id) {
        try {
            Field f = null;
            Class<?> c = target.getClass();
            while (c != null && f == null) {
                try { f = c.getDeclaredField("id"); } catch (NoSuchFieldException ignored) {}
                c = c.getSuperclass();
            }
            if (f == null) throw new RuntimeException("No id field");
            f.setAccessible(true);
            f.set(target, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
