package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.ReservationRequest;
import com.bookmania.bookmania.Dtos.ReservationResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Reservation;
import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Enums.ReservationStatus;
import com.bookmania.bookmania.Repository.BookRepository;
import com.bookmania.bookmania.Repository.ReservationRepository;
import com.bookmania.bookmania.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private static final int MAX_QUEUE_SIZE = 3;
    private static final int PICKUP_DAYS = 3; // días para recoger cuando le toca

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    // ── BOOK-29: crear reserva ──────────────────────────────────────────────
    public ReservationResponse create(ReservationRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // BOOK-33: bloquear si tiene penalización activa
        if (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now())) {
            throw new RuntimeException(
                "Tienes una penalización activa hasta " + user.getPenaltyUntil() + ". No puedes hacer reservas."
            );
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));

        // Si hay copias disponibles no tiene sentido reservar
        if (book.getAvailableCopies() > 0) {
            throw new RuntimeException("El libro está disponible. Solicita un préstamo directamente.");
        }

        // No permitir reserva duplicada
        if (reservationRepository.existsByUserIdAndBookIdAndStatus(
                user.getId(), book.getId(), ReservationStatus.PENDING)) {
            throw new RuntimeException("Ya tienes una reserva activa para este libro.");
        }

        // BOOK-29: máximo 3 en cola
        long queueSize = reservationRepository.countByBookIdAndStatus(book.getId(), ReservationStatus.PENDING);
        if (queueSize >= MAX_QUEUE_SIZE) {
            throw new RuntimeException(
                "La cola de espera está llena (máximo " + MAX_QUEUE_SIZE + " personas)."
            );
        }

        Reservation reservation = Reservation.builder()
                .user(user)
                .book(book)
                .queuePosition((int) queueSize + 1)
                .status(ReservationStatus.PENDING)
                .build();

        return toResponse(reservationRepository.save(reservation));
    }

    // ── BOOK-31: cancelar reserva ───────────────────────────────────────────
    public ReservationResponse cancel(Long reservationId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para cancelar esta reserva");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new RuntimeException("Solo se pueden cancelar reservas pendientes");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        // Reordenar cola: restar 1 a los que estaban detrás
        reorderQueue(reservation.getBook().getId(), reservation.getQueuePosition());

        return toResponse(saved);
    }

    // ── BOOK-32: mis reservas ───────────────────────────────────────────────
    public List<ReservationResponse> getMyReservations() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return reservationRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── BOOK-30: asignación automática al devolver libro ────────────────────
    // Llamado desde LoanService.returnBook()
    public void notifyNextInQueue(Long bookId) {
        List<Reservation> queue = reservationRepository
                .findByBookIdAndStatusOrderByQueuePositionAsc(bookId, ReservationStatus.PENDING);

        if (queue.isEmpty()) return;

        // Asignar fecha límite de recogida al primero de la cola
        Reservation first = queue.get(0);
        first.setExpiryDate(LocalDate.now().plusDays(PICKUP_DAYS));
        reservationRepository.save(first);
    }

    // Llamado desde LoanService.create() — marca la reserva como cumplida
    public void fulfillReservation(Long userId, Long bookId) {
        reservationRepository
                .findByUserIdAndBookIdAndStatus(userId, bookId, ReservationStatus.PENDING)
                .ifPresent(r -> {
                    r.setStatus(ReservationStatus.FULFILLED);
                    reservationRepository.save(r);
                });
    }

    private void reorderQueue(Long bookId, int cancelledPosition) {
        List<Reservation> remaining = reservationRepository
                .findByBookIdAndStatusOrderByQueuePositionAsc(bookId, ReservationStatus.PENDING);

        for (Reservation r : remaining) {
            if (r.getQueuePosition() > cancelledPosition) {
                r.setQueuePosition(r.getQueuePosition() - 1);
                reservationRepository.save(r);
            }
        }
    }

    private ReservationResponse toResponse(Reservation r) {
        return new ReservationResponse(
                r.getId(),
                r.getUser().getName(),
                r.getBook().getTitle(),
                r.getQueuePosition(),
                r.getReservationDate(),
                r.getExpiryDate(),
                r.getStatus(),
                r.getCreatedAt()
        );
    }
}