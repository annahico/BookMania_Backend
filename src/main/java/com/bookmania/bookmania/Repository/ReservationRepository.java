package com.bookmania.bookmania.Repository;

import com.bookmania.bookmania.Entity.Reservation;
import com.bookmania.bookmania.Enums.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByBookIdAndStatusOrderByQueuePositionAsc(Long bookId, ReservationStatus status);
    List<Reservation> findByUserId(Long userId);
    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, ReservationStatus status);
    long countByBookIdAndStatus(Long bookId, ReservationStatus status);
    Optional<Reservation> findByUserIdAndBookIdAndStatus(Long userId, Long bookId, ReservationStatus status);
}