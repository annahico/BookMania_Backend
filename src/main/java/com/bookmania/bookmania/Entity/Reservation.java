package com.bookmania.bookmania.Entity;

import com.bookmania.bookmania.Enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    // posición en la cola: 1, 2 o 3
    @Column(nullable = false)
    private Integer queuePosition;

    @Column(nullable = false)
    private LocalDate reservationDate;

    // fecha límite para recoger el libro cuando esté disponible
    @Column
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.reservationDate = LocalDate.now();
        this.status = ReservationStatus.PENDING;
    }
}
