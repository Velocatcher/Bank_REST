package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card,Long> {
    Page<Card> findByOwner(User owner, Pageable pageable);
    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);
    Page<Card> findByOwnerAndLast4Containing(User owner, String last4, Pageable pageable);

    Optional<Card> findByIdAndOwnerId(Long id, Long ownerId);
}


