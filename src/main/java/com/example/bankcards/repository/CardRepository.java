package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CardRepository extends JpaRepository<Card, Long> {

    // ВАЖНО: везде подгружаем owner, чтобы не было LazyInitializationException
    @EntityGraph(attributePaths = "owner")
    Page<Card> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Optional<Card> findById(Long id);

    @EntityGraph(attributePaths = "owner")
    Page<Card> findByOwner(User owner, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Card> findByOwnerAndStatus(User owner, CardStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Page<Card> findByOwnerAndLast4Containing(User owner, String last4, Pageable pageable);

    @EntityGraph(attributePaths = "owner")
    Optional<Card> findByIdAndOwnerId(Long id, Long ownerId);
}
