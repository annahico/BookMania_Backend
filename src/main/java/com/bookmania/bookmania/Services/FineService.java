package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.FineResponse;
import com.bookmania.bookmania.Entity.Fine;
import com.bookmania.bookmania.Entity.Loan;
import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
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

    private static final int BASE_PENALTY_DAYS = 30; //modificar segons política de la biblioteca
    private static final int EXTRA_DAYS_PER_OVERDUE_DAY = 1; //modificar segons política de la biblioteca

    private final FineRepository fineRepository;
    private final UserRepository userRepository;

    public void generateFine(Loan loan) {
        if (fineRepository.existsByLoanId(loan.getId())) {
            return;
        }

        long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());

        if (daysOverdue <= 0) {
            System.out.println(">>> daysOverdue <= 0, saliendo sin generar multa");
            return;
        }

        int penaltyDays = BASE_PENALTY_DAYS + (int) (daysOverdue * EXTRA_DAYS_PER_OVERDUE_DAY);

        User user = userRepository.findById(loan.getUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        LocalDate baseDate = (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now()))
                ? user.getPenaltyUntil()
                : LocalDate.now();

        LocalDate penaltyUntil = baseDate.plusDays(penaltyDays);

        user.setPenaltyUntil(penaltyUntil);
        userRepository.save(user);

        Fine fine = Fine.builder()
                .loan(loan)
                .user(user)
                .daysOverdue((int) daysOverdue)
                .penaltyDays(penaltyDays)
                .penaltyUntil(penaltyUntil)
                .build();
        fineRepository.save(fine);
    }

    public List<FineResponse> getMyFines() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        return fineRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<FineResponse> getAllFines() {
        return fineRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public void delete(Long id) {
        Fine fine = fineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Multa no encontrada"));

        User user = fine.getUser();
        if (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now())) {
            user.setPenaltyUntil(null);
            userRepository.save(user);
        }

        fineRepository.deleteById(id);
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
                fine.getUser().getName(),
                fine.getDaysOverdue(),
                fine.getPenaltyDays(),
                fine.getPenaltyUntil(),
                daysRemaining,
                fine.getCreatedAt()
        );
    }
}
