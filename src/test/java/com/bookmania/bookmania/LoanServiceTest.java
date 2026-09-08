package com.bookmania.bookmania;

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
import com.bookmania.bookmania.Security.CurrentUserService;
import com.bookmania.bookmania.Services.FineService;
import com.bookmania.bookmania.Services.LoanService;
import com.bookmania.bookmania.Services.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock
    private LoanRepository loanRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private FineService fineService;
    @Mock
    private ReservationService reservationService;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private LoanService loanService;

    private User user;
    private Book book;
    private Loan loan;
    private LoanRequest request;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Test User").email("user@test.com").penaltyUntil(null).build();
        lenient().when(currentUserService.getCurrentUser()).thenReturn(user);

        book = Book.builder().id(10L).title("Clean Code").availableCopies(1).build();

        loan = new Loan();
        loan.setId(100L);
        loan.setUser(user);
        loan.setBook(book);
        loan.setDueDate(LocalDate.now().plusDays(21));
        loan.setStatus(LoanStatus.ISSUED);
        loan.setExtensionsUsed(0);

        request = new LoanRequest();
        request.setBookId(10L);
    }

    @Test
    void create_success_decrementsAvailableCopiesAndReturnsResponse() {
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.ISSUED)).thenReturn(0L);
        when(loanRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, LoanStatus.ISSUED)).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        LoanResponse response = loanService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getBookTitle()).isEqualTo("Clean Code");
        assertThat(book.getAvailableCopies()).isEqualTo(0);
        verify(reservationService).fulfillReservation(1L, 10L);
    }

    @Test
    void create_userWithActivePenalty_throwsBusinessException() {
        user.setPenaltyUntil(LocalDate.now().plusDays(5));

        assertThatThrownBy(() -> loanService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("penalización activa");
    }

    @Test
    void create_expiredPenalty_isClearedAndLoanProceeds() {
        user.setPenaltyUntil(LocalDate.now().minusDays(5));
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.ISSUED)).thenReturn(0L);
        when(loanRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, LoanStatus.ISSUED)).thenReturn(false);
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        loanService.create(request);

        assertThat(user.getPenaltyUntil()).isNull();
    }

    @Test
    void create_atLoanLimit_throwsBusinessException() {
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.ISSUED)).thenReturn(7L);

        assertThatThrownBy(() -> loanService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("límite");
    }

    @Test
    void create_bookNotFound_throwsResourceNotFoundException() {
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.ISSUED)).thenReturn(0L);
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_noAvailableCopies_throwsBusinessException() {
        book.setAvailableCopies(0);
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.ISSUED)).thenReturn(0L);
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> loanService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("copias disponibles");
    }

    @Test
    void create_alreadyBorrowed_throwsBusinessException() {
        when(loanRepository.countByUserIdAndStatus(1L, LoanStatus.ISSUED)).thenReturn(0L);
        when(bookRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(book));
        when(loanRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, LoanStatus.ISSUED)).thenReturn(true);

        assertThatThrownBy(() -> loanService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("en préstamo");
    }

    @Test
    void getMyLoans_returnsUserLoans() {
        when(loanRepository.findByUserId(1L)).thenReturn(List.of(loan));

        List<LoanResponse> result = loanService.getMyLoans();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookTitle()).isEqualTo("Clean Code");
    }

    @Test
    void extend_owner_success_increasesDueDateAndExtensionsUsed() {
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);
        LocalDate originalDueDate = loan.getDueDate();

        loanService.extend(100L);

        assertThat(loan.getDueDate()).isEqualTo(originalDueDate.plusDays(10));
        assertThat(loan.getExtensionsUsed()).isEqualTo(1);
    }

    @Test
    void extend_differentUser_throwsForbiddenException() {
        User otherUser = User.builder().id(99L).email("other@test.com").build();
        loan.setUser(otherUser);
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.extend(100L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void extend_admin_canExtendAnyonesLoan() {
        User otherUser = User.builder().id(99L).email("other@test.com").build();
        loan.setUser(otherUser);
        when(currentUserService.isCurrentUserAdmin()).thenReturn(true);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        assertThat(loanService.extend(100L)).isNotNull();
    }

    @Test
    void extend_alreadyReturned_throwsBusinessException() {
        loan.setStatus(LoanStatus.RETURNED);
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.extend(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("devuelto");
    }

    @Test
    void extend_overdue_throwsBusinessException() {
        loan.setStatus(LoanStatus.OVERDUE);
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.extend(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vencido");
    }

    @Test
    void extend_maxExtensionsReached_throwsBusinessException() {
        loan.setExtensionsUsed(3);
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.extend(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("prórrogas");
    }

    @Test
    void extend_loanNotFound_throwsResourceNotFoundException() {
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loanService.extend(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void returnBook_onTime_incrementsAvailableCopies() {
        book.setAvailableCopies(0);
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        loanService.returnBook(100L);

        assertThat(loan.getStatus()).isEqualTo(LoanStatus.RETURNED);
        assertThat(book.getAvailableCopies()).isEqualTo(1);
        verify(fineService, never()).generateFine(any());
        verify(reservationService).notifyNextInQueue(10L);
    }

    @Test
    void returnBook_overdue_generatesFine() {
        loan.setDueDate(LocalDate.now().minusDays(3));
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));
        when(loanRepository.save(any(Loan.class))).thenReturn(loan);

        loanService.returnBook(100L);

        verify(fineService).generateFine(loan);
    }

    @Test
    void returnBook_alreadyReturned_throwsBusinessException() {
        loan.setStatus(LoanStatus.RETURNED);
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnBook(100L))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void returnBook_differentUser_throwsForbiddenException() {
        User otherUser = User.builder().id(99L).email("other@test.com").build();
        loan.setUser(otherUser);
        when(currentUserService.isCurrentUserAdmin()).thenReturn(false);
        when(loanRepository.findById(100L)).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> loanService.returnBook(100L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getAllLoans_returnsAllLoans() {
        when(loanRepository.findAll()).thenReturn(List.of(loan));

        List<LoanResponse> result = loanService.getAllLoans();

        assertThat(result).hasSize(1);
    }
}
