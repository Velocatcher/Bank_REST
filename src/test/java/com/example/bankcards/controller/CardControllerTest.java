package com.example.bankcards.controller;

import com.example.bankcards.dto.card.CardCreateRequest;
import com.example.bankcards.dto.card.CardResponse;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CardControllerTest {

    @Mock
    private CardService cardService;

    @Mock
    private UserDetails userDetails;

    @Mock
    private UserDetails adminUserDetails;

    @InjectMocks
    private CardController cardController;

    private Card testCard;

    @BeforeEach
    void setUp() {
        testCard = mock(Card.class);
        when(testCard.getId()).thenReturn(1L);
        when(testCard.getExpiry()).thenReturn("12/25");
        when(testCard.getBalance()).thenReturn(new BigDecimal("1000.00"));
        when(testCard.getStatus()).thenReturn(CardStatus.ACTIVE);
    }

    @Test
    void create_WithValidRequest_ShouldReturnCardResponse() {
        // Arrange
        CardCreateRequest request = new CardCreateRequest(
                "1234567812345678",
                "12/25",
                new BigDecimal("1000.00"),
                "testUser"
        );

        when(cardService.create(
                request.number(),
                request.expiry(),
                request.ownerUsername(),
                request.initialBalance()
        )).thenReturn(testCard);

        when(cardService.masked(testCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(testCard)).thenReturn(CardStatus.ACTIVE);
        when(testCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(testCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        CardResponse response = cardController.create(request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("****5678", response.maskedNumber());
        assertEquals(CardStatus.ACTIVE, response.status());

        verify(cardService).create(
                request.number(),
                request.expiry(),
                request.ownerUsername(),
                request.initialBalance()
        );
    }

    @Test
    void list_ForAdminUser_ShouldReturnAllCards() {
        // Arrange
        int page = 0;
        int size = 10;
        Page<Card> cardPage = new PageImpl<>(List.of(testCard), PageRequest.of(page, size), 1);

        when(adminUserDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(cardService.listAll(page, size)).thenReturn(cardPage);
        when(cardService.masked(testCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(testCard)).thenReturn(CardStatus.ACTIVE);
        when(testCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(testCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        Page<CardResponse> response = cardController.list(adminUserDetails, null, null, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(cardService).listAll(page, size);
        verify(cardService, never()).listOwned(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void list_ForRegularUser_ShouldReturnOwnedCards() {
        // Arrange
        int page = 0;
        int size = 10;
        String username = "testUser";
        CardStatus status = CardStatus.ACTIVE;
        String last4 = "5678";
        Page<Card> cardPage = new PageImpl<>(List.of(testCard), PageRequest.of(page, size), 1);

        when(userDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetails.getUsername()).thenReturn(username);

        when(cardService.listOwned(username, status, last4, page, size)).thenReturn(cardPage);
        when(cardService.masked(testCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(testCard)).thenReturn(CardStatus.ACTIVE);
        when(testCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(testCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        Page<CardResponse> response = cardController.list(userDetails, status, last4, page, size);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        verify(cardService).listOwned(username, status, last4, page, size);
        verify(cardService, never()).listAll(anyInt(), anyInt());
    }

    @Test
    void list_WithNegativePage_ShouldThrowBadRequestException() {
        // Arrange
        when(userDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            cardController.list(userDetails, null, null, -1, 10);
        });
    }

    @Test
    void list_WithInvalidSize_ShouldThrowBadRequestException() {
        // Arrange
        when(userDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));

        // Act & Assert
        assertThrows(BadRequestException.class, () -> {
            cardController.list(userDetails, null, null, 0, 0);
        });
    }

    @Test
    void get_ForAdminUser_ShouldReturnAnyCard() {
        // Arrange
        Long cardId = 1L;

        when(adminUserDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(cardService.findByIdOr404(cardId)).thenReturn(testCard);
        when(cardService.masked(testCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(testCard)).thenReturn(CardStatus.ACTIVE);
        when(testCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(testCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        CardResponse response = cardController.get(cardId, adminUserDetails);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(cardService).findByIdOr404(cardId);
        verify(cardService, never()).getOwned(anyLong(), anyString());
    }

    @Test
    void get_ForRegularUser_ShouldReturnOwnedCard() {
        // Arrange
        Long cardId = 1L;
        String username = "testUser";

        when(userDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetails.getUsername()).thenReturn(username);

        when(cardService.getOwned(cardId, username)).thenReturn(testCard);
        when(cardService.masked(testCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(testCard)).thenReturn(CardStatus.ACTIVE);
        when(testCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(testCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        CardResponse response = cardController.get(cardId, userDetails);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.id());
        verify(cardService).getOwned(cardId, username);
        verify(cardService, never()).findByIdOr404(anyLong());
    }

    @Test
    void block_ForAdminUser_ShouldBlockAnyCard() {
        // Arrange
        Long cardId = 1L;
        Card blockedCard = mock(Card.class);
        when(blockedCard.getId()).thenReturn(cardId);
        when(blockedCard.getStatus()).thenReturn(CardStatus.BLOCKED);

        when(adminUserDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(cardService.block(cardId)).thenReturn(blockedCard);
        when(cardService.masked(blockedCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(blockedCard)).thenReturn(CardStatus.BLOCKED);
        when(blockedCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(blockedCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        CardResponse response = cardController.block(cardId, adminUserDetails);

        // Assert
        assertNotNull(response);
        verify(cardService).block(cardId);
        verify(cardService, never()).getOwned(anyLong(), anyString());
    }

    @Test
    void block_ForRegularUser_ShouldBlockOwnedCard() {
        // Arrange
        Long cardId = 1L;
        String username = "testUser";
        Card blockedCard = mock(Card.class);
        when(blockedCard.getId()).thenReturn(cardId);
        when(blockedCard.getStatus()).thenReturn(CardStatus.BLOCKED);

        when(userDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetails.getUsername()).thenReturn(username);

        when(cardService.getOwned(cardId, username)).thenReturn(testCard);
        when(cardService.block(cardId)).thenReturn(blockedCard);
        when(cardService.masked(blockedCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(blockedCard)).thenReturn(CardStatus.BLOCKED);
        when(blockedCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(blockedCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        CardResponse response = cardController.block(cardId, userDetails);

        // Assert
        assertNotNull(response);
        verify(cardService).getOwned(cardId, username);
        verify(cardService).block(cardId);
    }

    @Test
    void activate_AsAdmin_ShouldActivateCard() {
        // Arrange
        Long cardId = 1L;
        Card activatedCard = mock(Card.class);
        when(activatedCard.getId()).thenReturn(cardId);
        when(activatedCard.getStatus()).thenReturn(CardStatus.ACTIVE);

        when(cardService.activate(cardId)).thenReturn(activatedCard);
        when(cardService.masked(activatedCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(activatedCard)).thenReturn(CardStatus.ACTIVE);
        when(activatedCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(activatedCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        CardResponse response = cardController.activate(cardId);

        // Assert
        assertNotNull(response);
        verify(cardService).activate(cardId);
    }

    @Test
    void delete_AsAdmin_ShouldDeleteCard() {
        // Arrange
        Long cardId = 1L;

        // Act
        cardController.delete(cardId);

        // Assert
        verify(cardService).delete(cardId);
    }

    @Test
    void list_ForAdminUser_ShouldCallListAll() {
        // Arrange
        int page = 0;
        int size = 10;
        Page<Card> cardPage = new PageImpl<>(List.of(testCard), PageRequest.of(page, size), 1);

        when(adminUserDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN")));

        when(cardService.listAll(page, size)).thenReturn(cardPage);
        when(cardService.masked(testCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(testCard)).thenReturn(CardStatus.ACTIVE);
        when(testCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(testCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        Page<CardResponse> response = cardController.list(adminUserDetails, null, null, page, size);

        // Assert - проверяем, что вызвался listAll, что означает, что isAdmin вернул true
        verify(cardService).listAll(page, size);
    }

    @Test
    void list_ForRegularUser_ShouldCallListOwned() {
        // Arrange
        int page = 0;
        int size = 10;
        String username = "testUser";
        Page<Card> cardPage = new PageImpl<>(List.of(testCard), PageRequest.of(page, size), 1);

        when(userDetails.getAuthorities()).thenAnswer(invocation ->
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
        when(userDetails.getUsername()).thenReturn(username);

        when(cardService.listOwned(username, null, null, page, size)).thenReturn(cardPage);
        when(cardService.masked(testCard)).thenReturn("****5678");
        when(cardService.effectiveStatus(testCard)).thenReturn(CardStatus.ACTIVE);
        when(testCard.getOwner()).thenReturn(mock(com.example.bankcards.entity.User.class));
        when(testCard.getOwner().getUsername()).thenReturn("testUser");

        // Act
        Page<CardResponse> response = cardController.list(userDetails, null, null, page, size);

        // Assert - проверяем, что вызвался listOwned, что означает, что isAdmin вернул false
        verify(cardService).listOwned(username, null, null, page, size);
    }
}