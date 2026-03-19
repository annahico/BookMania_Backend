package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.LoanRequest;
import com.bookmania.bookmania.Dtos.LoanResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Fine;
import com.bookmania.bookmania.Entity.Loan;
import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Enums.LoanStatus;
import com.bookmania.bookmania.Repository.BookRepository;
import com.bookmania.bookmania.Repository.FineRepository;
import com.bookmania.bookmania.Repository.LoanRepository;
import com.bookmania.bookmania.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LoanService {

    private static final BigDecimal DAILY_FINE_RATE = new BigDecimal("1.00");

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;
    private final FineRepository fineRepository;

    // ── BOOK-20: emitir préstamo ────────────────────────────────────────────
    public LoanResponse create(LoanRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now())) {
            throw new RuntimeException("Tienes una penalización activa hasta " + user.getPenaltyUntil());
        }

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));

        if (book.getAvailableCopies() <= 0) {
            throw new RuntimeException("No hay copias disponibles de este libro");
        }

        if (loanRepository.existsByUserIdAndBookIdAndStatus(user.getId(), book.getId(), LoanStatus.ISSUED)) {
            throw new RuntimeException("Ya tienes este libro en préstamo");
        }

        Loan loan = new Loan();
        loan.setUser(user);
        loan.setBook(book);

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        bookRepository.save(book);

        return toResponse(loanRepository.save(loan));
    }

    // ── BOOK-21: historial préstamos ────────────────────────────────────────
    public List<LoanResponse> getMyLoans() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return loanRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    // ── BOOK-22: prorrogar préstamo ─────────────────────────────────────────
    public LoanResponse extend(Long loanId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para prorrogar este préstamo");
        }

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new RuntimeException("No se puede prorrogar un préstamo ya devuelto");
        }

        if (loan.getStatus() == LoanStatus.OVERDUE) {
            throw new RuntimeException("El préstamo está vencido. Paga la multa antes de prorrogar.");
        }

        if (loan.getExtensionsUsed() >= 3) {
            throw new RuntimeException("Has alcanzado el máximo de prórrogas permitidas (3)");
        }

        // BOOK-27: bloquear si tiene multa pendiente
        if (fineRepository.existsByUserIdAndPaidFalse(user.getId())) {
            throw new RuntimeException("Tienes una multa pendiente. Págala antes de prorrogar.");
        }

        if (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now())) {
            throw new RuntimeException("Tienes una penalización activa hasta " + user.getPenaltyUntil());
        }

        // Las prórrogas se calculan desde la dueDate actual (no desde hoy)
        loan.setDueDate(loan.getDueDate().plusDays(10));
        loan.setExtensionsUsed(loan.getExtensionsUsed() + 1);

        return toResponse(loanRepository.save(loan));
    }

    // ── BOOK-23: devolver libro ─────────────────────────────────────────────
    public LoanResponse returnBook(Long loanId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para devolver este préstamo");
        }

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new RuntimeException("Este préstamo ya fue devuelto");
        }

        // Si está vencido, generar multa y bloquear hasta que se pague
        if (LocalDate.now().isAfter(loan.getDueDate())) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);

            // Crear multa si no existe ya
            boolean fineExists = fineRepository.existsByLoanIdAndPaidFalse(loan.getId());
            if (!fineExists) {
                long daysOverdue = ChronoUnit.DAYS.between(loan.getDueDate(), LocalDate.now());
                BigDecimal amount = DAILY_FINE_RATE.multiply(BigDecimal.valueOf(daysOverdue));

                Fine fine = Fine.builder()
                        .loan(loan)
                        .user(user)
                        .amount(amount)
                        .paid(false)
                        .build();
                fineRepository.save(fine);
            }

            throw new RuntimeException(
                "El préstamo está vencido. Tienes una multa pendiente. Págala para poder devolver el libro."
            );
        }

        // Devolución normal
        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now());

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        return toResponse(loanRepository.save(loan));
    }

    // ── BOOK-24: usado por el Scheduler (se llama desde LoanScheduler) ──────
    public void markOverdueLoans() {
        List<Loan> active = loanRepository.findByStatus(LoanStatus.ISSUED);
        for (Loan loan : active) {
            if (LocalDate.now().isAfter(loan.getDueDate())) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
            }
        }
    }

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getUser().getName(),
                loan.getBook().getTitle(),
                loan.getIssueDate(),
                loan.getDueDate(),
                loan.getReturnDate(),
                loan.getExtensionsUsed(),
                loan.getStatus()
        );
    }
}