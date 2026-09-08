package com.bookmania.bookmania;

import com.bookmania.bookmania.Dtos.FineResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Fine;
import com.bookmania.bookmania.Entity.Loan;
import com.bookmania.bookmania.Entity.User;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
import com.bookmania.bookmania.Repository.FineRepository;
import com.bookmania.bookmania.Repository.UserRepository;
import com.bookmania.bookmania.Security.CurrentUserService;
import com.bookmania.bookmania.Services.FineService;
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
class FineServiceTest {

    @Mock
    private FineRepository fineRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private FineService fineService;

    private User user;
    private Loan loan;
    private Fine fine;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).name("Test User").email("user@test.com").penaltyUntil(null).build();

        Book book = Book.builder().id(10L).title("Clean Code").build();

        loan = new Loan();
        loan.setId(100L);
        loan.setUser(user);
        loan.setBook(book);
        loan.setDueDate(LocalDate.now().minusDays(5));

        fine = Fine.builder()
                .id(1L)
                .loan(loan)
                .user(user)
                .daysOverdue(5)
                .penaltyDays(35)
                .penaltyUntil(LocalDate.now().plusDays(30))
                .build();
    }

    @Test
    void generateFine_alreadyFined_doesNothing() {
        when(fineRepository.existsByLoanId(100L)).thenReturn(true);

        fineService.generateFine(loan);

        verify(fineRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void generateFine_notOverdueYet_doesNothing() {
        loan.setDueDate(LocalDate.now());
        when(fineRepository.existsByLoanId(100L)).thenReturn(false);

        fineService.generateFine(loan);

        verify(fineRepository, never()).save(any());
    }

    @Test
    void generateFine_overdue_setsPenaltyUntilBasedOnDaysOverdue() {
        // BASE_PENALTY_DAYS(30) + EXTRA_DAYS_PER_OVERDUE_DAY(1) * daysOverdue(5) = 35
        when(fineRepository.existsByLoanId(100L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        fineService.generateFine(loan);

        LocalDate expected = LocalDate.now().plusDays(35);
        assertThat(user.getPenaltyUntil()).isEqualTo(expected);
        verify(fineRepository).save(argThat(f ->
                f.getDaysOverdue() == 5 && f.getPenaltyDays() == 35 && f.getPenaltyUntil().equals(expected)
        ));
    }

    @Test
    void generateFine_userAlreadyPenalized_extendsFromExistingPenalty() {
        LocalDate existingPenalty = LocalDate.now().plusDays(10);
        user.setPenaltyUntil(existingPenalty);
        when(fineRepository.existsByLoanId(100L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        fineService.generateFine(loan);

        assertThat(user.getPenaltyUntil()).isEqualTo(existingPenalty.plusDays(35));
    }

    @Test
    void generateFine_userNotFound_throwsResourceNotFoundException() {
        when(fineRepository.existsByLoanId(100L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fineService.generateFine(loan))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getMyFines_returnsCurrentUsersFines() {
        when(currentUserService.getCurrentUser()).thenReturn(user);
        when(fineRepository.findByUserId(1L)).thenReturn(List.of(fine));

        List<FineResponse> result = fineService.getMyFines();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookTitle()).isEqualTo("Clean Code");
    }

    @Test
    void getAllFines_returnsAllFines() {
        when(fineRepository.findAll()).thenReturn(List.of(fine));

        List<FineResponse> result = fineService.getAllFines();

        assertThat(result).hasSize(1);
    }

    @Test
    void delete_activePenalty_clearsPenaltyAndDeletesFine() {
        user.setPenaltyUntil(LocalDate.now().plusDays(10));
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        fineService.delete(1L);

        assertThat(user.getPenaltyUntil()).isNull();
        verify(fineRepository).deleteById(1L);
    }

    @Test
    void delete_noActivePenalty_justDeletesFine() {
        when(fineRepository.findById(1L)).thenReturn(Optional.of(fine));

        fineService.delete(1L);

        verify(userRepository, never()).save(any());
        verify(fineRepository).deleteById(1L);
    }

    @Test
    void delete_fineNotFound_throwsResourceNotFoundException() {
        when(fineRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fineService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
