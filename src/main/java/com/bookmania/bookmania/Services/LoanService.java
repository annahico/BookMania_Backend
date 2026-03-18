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
        // Obtener usuario autenticado
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Verificar si el usuario está bloqueado por multa
        if (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now())) {
            throw new RuntimeException("Tienes una penalización activa hasta " + user.getPenaltyUntil());
        }

        // Verificar que el libro existe
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new RuntimeException("Libro no encontrado"));

        // Verificar que hay copias disponibles
        if (book.getAvailableCopies() <= 0) {
            throw new RuntimeException("No hay copias disponibles de este libro");
        }

        // Verificar que el usuario no tiene ya este libro en préstamo
        if (loanRepository.existsByUserIdAndBookIdAndStatus(user.getId(), book.getId(), LoanStatus.ISSUED)) {
            throw new RuntimeException("Ya tienes este libro en préstamo");
        }

        // Crear el préstamo
        Loan loan = new Loan();
        loan.setUser(user);
        loan.setBook(book);

        // Reducir copias disponibles
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
