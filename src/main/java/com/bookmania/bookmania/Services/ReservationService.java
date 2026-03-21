package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.ReservationRequest;
import com.bookmania.bookmania.Dtos.ReservationResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Reservation;
import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Enums.ReservationStatus;
import com.bookmania.bookmania.Exception.BusinessException;
import com.bookmania.bookmania.Exception.ForbiddenException;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
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
    private static final int PICKUP_DAYS = 3;

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    public ReservationResponse create(ReservationRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        if (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now())) {
            throw new BusinessException(
                    "Tienes una penalización activa hasta " + user.getPenaltyUntil() + ". No puedes hacer reservas."
            );
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));

        if (book.getAvailableCopies() > 0) {
            throw new BusinessException("El libro está disponible. Solicita un préstamo directamente.");
        }

        if (reservationRepository.existsByUserIdAndBookIdAndStatus(
                user.getId(), book.getId(), ReservationStatus.PENDING)) {
            throw new BusinessException("Ya tienes una reserva activa para este libro.");
        }

        long queueSize = reservationRepository.countByBookIdAndStatus(book.getId(), ReservationStatus.PENDING);
        if (queueSize >= MAX_QUEUE_SIZE) {
            throw new BusinessException(
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

    public ReservationResponse cancel(Long reservationId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva no encontrada"));

        if (!reservation.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("No tienes permiso para cancelar esta reserva");
        }

        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new BusinessException("Solo se pueden cancelar reservas pendientes");
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        Reservation saved = reservationRepository.save(reservation);

        reorderQueue(reservation.getBook().getId(), reservation.getQueuePosition());

        return toResponse(saved);
    }

    public List<ReservationResponse> getMyReservations() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return reservationRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public void notifyNextInQueue(Long bookId) {
        List<Reservation> queue = reservationRepository
                .findByBookIdAndStatusOrderByQueuePositionAsc(bookId, ReservationStatus.PENDING);

        if (queue.isEmpty()) {
            return;
        }

        Reservation first = queue.get(0);
        first.setExpiryDate(LocalDate.now().plusDays(PICKUP_DAYS));
        reservationRepository.save(first);
    }

    public void fulfillReservation(Long userId, Long bookId) {
        reservationRepository
                .findByUserIdAndBookIdAndStatus(userId, bookId, ReservationStatus.PENDING)
                .ifPresent(r -> {
                    r.setStatus(ReservationStatus.FULFILLED);
                    reservationRepository.save(r);
                });
    }

    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
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
