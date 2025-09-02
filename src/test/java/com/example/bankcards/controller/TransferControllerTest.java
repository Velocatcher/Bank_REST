package com.example.bankcards.controller;

import com.example.bankcards.dto.transfer.TransferRequest;
import com.example.bankcards.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    @Mock
    private TransferService transferService;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private TransferController transferController;

    private TransferRequest validTransferRequest;

    @BeforeEach
    void setUp() {
        validTransferRequest = new TransferRequest(1L, 2L, new BigDecimal("100.00"));
    }

    @Test
    void transfer_WithValidRequest_ShouldReturnOkWithTransactionId() {
        // Arrange
        String username = "testUser";
        Long expectedTransactionId = 123L;

        when(userDetails.getUsername()).thenReturn(username);
        when(transferService.transfer(username, validTransferRequest.fromCardId(),
                validTransferRequest.toCardId(), validTransferRequest.amount()))
                .thenReturn(expectedTransactionId);

        // Act
        ResponseEntity<Long> response = transferController.transfer(userDetails, validTransferRequest);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(expectedTransactionId, response.getBody());

        verify(userDetails).getUsername();
        verify(transferService).transfer(username, validTransferRequest.fromCardId(),
                validTransferRequest.toCardId(), validTransferRequest.amount());
        verifyNoMoreInteractions(transferService);
    }

    @Test
    void transfer_ShouldCallServiceWithCorrectParameters() {
        // Arrange
        String username = "john.doe";
        Long fromCardId = 1L;
        Long toCardId = 2L;
        BigDecimal amount = new BigDecimal("500.50");

        TransferRequest request = new TransferRequest(fromCardId, toCardId, amount);

        when(userDetails.getUsername()).thenReturn(username);
        when(transferService.transfer(username, fromCardId, toCardId, amount))
                .thenReturn(456L);

        // Act
        transferController.transfer(userDetails, request);

        // Assert
        verify(transferService).transfer(username, fromCardId, toCardId, amount);
    }

    @Test
    void transfer_WithMinimumAmount_ShouldCallService() {
        // Arrange
        String username = "testUser";
        BigDecimal minimumAmount = new BigDecimal("0.01");
        TransferRequest request = new TransferRequest(1L, 2L, minimumAmount);

        when(userDetails.getUsername()).thenReturn(username);
        when(transferService.transfer(username, 1L, 2L, minimumAmount)).thenReturn(789L);

        // Act
        ResponseEntity<Long> response = transferController.transfer(userDetails, request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        verify(transferService).transfer(username, 1L, 2L, minimumAmount);
    }

    @Test
    void transfer_WithLargeAmount_ShouldCallService() {
        // Arrange
        String username = "testUser";
        BigDecimal largeAmount = new BigDecimal("1000000.99");
        TransferRequest request = new TransferRequest(1L, 2L, largeAmount);

        when(userDetails.getUsername()).thenReturn(username);
        when(transferService.transfer(username, 1L, 2L, largeAmount)).thenReturn(999L);

        // Act
        ResponseEntity<Long> response = transferController.transfer(userDetails, request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        verify(transferService).transfer(username, 1L, 2L, largeAmount);
    }

    @Test
    void transfer_WithPreciseDecimalAmount_ShouldCallService() {
        // Arrange
        String username = "testUser";
        BigDecimal preciseAmount = new BigDecimal("123.4567");
        TransferRequest request = new TransferRequest(1L, 2L, preciseAmount);

        when(userDetails.getUsername()).thenReturn(username);
        when(transferService.transfer(username, 1L, 2L, preciseAmount)).thenReturn(111L);

        // Act
        ResponseEntity<Long> response = transferController.transfer(userDetails, request);

        // Assert
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        verify(transferService).transfer(username, 1L, 2L, preciseAmount);
    }

    @Test
    void transfer_ShouldExtractUsernameFromUserDetails() {
        // Arrange
        String expectedUsername = "specificUser";
        when(userDetails.getUsername()).thenReturn(expectedUsername);
        when(transferService.transfer(expectedUsername, 1L, 2L, new BigDecimal("100.00"))).thenReturn(111L);

        // Act
        transferController.transfer(userDetails, validTransferRequest);

        // Assert
        verify(userDetails).getUsername();
        verify(transferService).transfer(expectedUsername, 1L, 2L, new BigDecimal("100.00"));
    }

    @Test
    void constructor_ShouldInitializeTransferService() {
        // This test verifies that the constructor properly sets the dependency
        TransferService mockService = mock(TransferService.class);
        TransferController controller = new TransferController(mockService);

        assertNotNull(controller);
    }
}