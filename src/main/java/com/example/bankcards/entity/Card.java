package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "cards")
public class Card {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    /**
     * Номер карты хранится шифрованным как Base64: "iv:cipher" (AES-GCM)
     * Почему: минимизируем риск утечки чувствительных данных из БД.
     */

    @Column(name = "enc_number", nullable = false, length = 512)
    private String encNumber;
    @Column(name = "last4", nullable = false, length = 4)
    private String last4;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;
    @Column(nullable = false, length = 5)
    private String expiry;

    @Column(nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private CardStatus status;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(nullable = false)
    private LocalDateTime createdAt;

}
