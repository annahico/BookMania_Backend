package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.LoanRequest;
import com.bookmania.bookmania.Dtos.LoanResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Loan;
import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Enums.LoanStatus;
import com.bookmania.bookmania.Repository.BookRepository;
import com.bookmania.bookmania.Repository.LoanRepository;
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
public class LoanService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

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

    public List<LoanResponse> getMyLoans() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        return loanRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public LoanResponse extend(Long loanId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("No tienes permiso para prorrogar este préstamo");
        }

        if (loan.getStatus() != LoanStatus.ISSUED) {
            throw new RuntimeException("Solo se pueden prorrogar préstamos activos");
        }

        if (loan.getExtensionsUsed() >= 3) {
            throw new RuntimeException("Has alcanzado el máximo de prórrogas permitidas (3)");
        }

        if (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now())) {
            throw new RuntimeException("Tienes una penalización activa hasta " + user.getPenaltyUntil());
        }

        loan.setDueDate(loan.getDueDate().plusDays(10));
        loan.setExtensionsUsed(loan.getExtensionsUsed() + 1);

        return toResponse(loanRepository.save(loan));
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