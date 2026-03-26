package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.LoanRequest;
import com.bookmania.bookmania.Dtos.LoanResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Loan;
import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Enums.LoanStatus;
import com.bookmania.bookmania.Exception.BusinessException;
import com.bookmania.bookmania.Exception.ForbiddenException;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
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
    private final FineService fineService;
    private final ReservationService reservationService;

   public LoanResponse create(LoanRequest request) {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    
    // Fuerza carga fresca desde BD
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

    if (user.getPenaltyUntil() != null && !LocalDate.now().isBefore(user.getPenaltyUntil().plusDays(1))) {
        // Limpia penaltyUntil si ya expiró
        user.setPenaltyUntil(null);
        userRepository.save(user);
    }

    if (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now())) {
        throw new BusinessException("Tienes una penalización activa hasta " + user.getPenaltyUntil());
    }

        long activeLoans = loanRepository.countByUserIdAndStatus(user.getId(), LoanStatus.ISSUED);
        if (activeLoans >= 7) {
            throw new BusinessException("Has alcanzado el límite de 7 préstamos activos simultáneos");
        }

        Book book = bookRepository.findById(request.getBookId())
                    .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
            
            if (book.getAvailableCopies() <= 0) {
            throw new BusinessException("No hay copias disponibles. Puedes hacer una reserva.");
        }

        if (loanRepository.existsByUserIdAndBookIdAndStatus(user.getId(), book.getId(), LoanStatus.ISSUED)) {
            throw new BusinessException("Ya tienes este libro en préstamo");
        }

        reservationService.fulfillReservation(user.getId(), book.getId());

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
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        return loanRepository.findByUserId(user.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    public LoanResponse extend(Long loanId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado"));

        if (!loan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("No tienes permiso para prorrogar este préstamo");
        }

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("No se puede prorrogar un préstamo ya devuelto");
        }

        if (loan.getStatus() == LoanStatus.OVERDUE) {
            throw new BusinessException("El préstamo está vencido. No puedes prorrogar.");
        }

        if (loan.getExtensionsUsed() >= 3) {
            throw new BusinessException("Has alcanzado el máximo de prórrogas permitidas (3)");
        }

public void delete(Long id) {
    Fine fine = fineRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Multa no encontrada"));

    User user = fine.getUser();
    fineRepository.deleteById(id);

    // Recalcula la penalización basándose en las multas restantes
    List<Fine> remainingFines = fineRepository.findByUserId(user.getId());
    LocalDate maxPenalty = remainingFines.stream()
            .map(Fine::getPenaltyUntil)
            .filter(date -> date.isAfter(LocalDate.now()))
            .max(LocalDate::compareTo)
            .orElse(null);

    user.setPenaltyUntil(maxPenalty);
    userRepository.save(user);
}

        if (user.getPenaltyUntil() != null && user.getPenaltyUntil().isAfter(LocalDate.now())) {
            throw new BusinessException("Tienes una penalización activa hasta " + user.getPenaltyUntil());
        }

        loan.setDueDate(loan.getDueDate().plusDays(10));
        loan.setExtensionsUsed(loan.getExtensionsUsed() + 1);

        return toResponse(loanRepository.save(loan));
    }

    public LoanResponse returnBook(Long loanId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado"));

        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !loan.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("No tienes permiso para devolver este préstamo");
        }

        if (loan.getStatus() == LoanStatus.RETURNED) {
            throw new BusinessException("Este préstamo ya fue devuelto");
        }

        if (LocalDate.now().isAfter(loan.getDueDate())) {
            loan.setStatus(LoanStatus.OVERDUE);
            loanRepository.save(loan);
            fineService.generateFine(loan);
        }

        loan.setStatus(LoanStatus.RETURNED);
        loan.setReturnDate(LocalDate.now());

        Book book = loan.getBook();
        book.setAvailableCopies(book.getAvailableCopies() + 1);
        bookRepository.save(book);

        reservationService.notifyNextInQueue(book.getId());

        return toResponse(loanRepository.save(loan));
    }

    public void markOverdueLoans() {
        List<Loan> active = loanRepository.findByStatus(LoanStatus.ISSUED);
        for (Loan loan : active) {
            if (LocalDate.now().isAfter(loan.getDueDate())) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
            }
        }
    }

    public List<LoanResponse> getAllLoans() {
        return loanRepository.findAll().stream()
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
