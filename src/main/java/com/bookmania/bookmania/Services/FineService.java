package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.FineResponse;
import com.bookmania.bookmania.Entity.Fine;
import com.bookmania.bookmania.Entity.Loan;
import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Repository.FineRepository;
import com.bookmania.bookmania.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FineService {

    private static final int BASE_PENALTY_DAYS = 7;
    private static final int EXTRA_DAYS_PER_OVERDUE_DAY = 2;

    private final FineRepository fineRepository;
    private final UserRepository userRepository;

    /**
     * Genera la penalización cuando un préstamo se devuelve vencido.
     * Llamado desde LoanService.returnBook() y desde LoanScheduler.
     */
    public void generateFine(Loan loan) {
        // No generar multa duplicada
        if (fineRepository.existsByLoanId(loan.getId())) return;

        long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
        if (daysOverdue <= 0) return;

        int penaltyDays = BASE_PENALTY_DAYS + (int) (daysOverdue * EXTRA_DAYS_PER_OVERDUE_DAY);

        // Acumular sobre penaltyUntil existente si ya tiene bloqueo activo
        User user = loan.getUser();
        LocalDate baseDate = (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now()))
                ? user.getPenaltyUntil()
                : LocalDate.now();

        LocalDate penaltyUntil = baseDate.plusDays(penaltyDays);

        // Actualizar el usuario
        user.setPenaltyUntil(penaltyUntil);
        userRepository.save(user);

        // Registrar la multa
        Fine fine = Fine.builder()
                .loan(loan)
                .user(user)
                .daysOverdue((int) daysOverdue)
                .penaltyDays(penaltyDays)
                .penaltyUntil(penaltyUntil)
                .build();
        fineRepository.save(fine);
    }

    /**
     * Mis multas — historial de penalizaciones del usuario autenticado.
     */
    public List<FineResponse> getMyFines() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        return fineRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private FineResponse toResponse(Fine fine) {
        long daysRemaining = 0;
        if (fine.getPenaltyUntil().isAfter(LocalDate.now())) {
            daysRemaining = ChronoUnit.DAYS.between(LocalDate.now(), fine.getPenaltyUntil());
        }

        return new FineResponse(
                fine.getId(),
                fine.getLoan().getId(),
                fine.getLoan().getBook().getTitle(),
                fine.getDaysOverdue(),
                fine.getPenaltyDays(),
                fine.getPenaltyUntil(),
                daysRemaining,
                fine.getCreatedAt()
        );
    }
}