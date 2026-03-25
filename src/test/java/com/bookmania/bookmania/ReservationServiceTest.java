package com.bookmania.bookmania;

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
import com.bookmania.bookmania.Services.ReservationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {

    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private Authentication authentication;
    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private ReservationService reservationService;

    private User user;
    private Book book;
    private Reservation reservation;
    private ReservationRequest request;

    @BeforeEach
    void setUp() {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getName()).thenReturn("user@test.com");
        SecurityContextHolder.setContext(securityContext);
        

        user = User.builder()
                .id(1L)
                .name("Test User")
                .email("user@test.com")
                .penaltyUntil(null)
                .build();

        book = Book.builder()
                .id(10L)
                .title("Clean Code")
                .availableCopies(0)
                .build();

        reservation = Reservation.builder()
                .id(100L)
                .user(user)
                .book(book)
                .queuePosition(1)
                .status(ReservationStatus.PENDING)
                .reservationDate(LocalDate.now())
                .build();

        request = new ReservationRequest();
        request.setBookId(10L);
    }
    // ── create ──────────────────────────────────────────────────────────────

    @Test
    void create_success_returnsReservationResponse() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, ReservationStatus.PENDING))
                .thenReturn(false);
        when(reservationRepository.countByBookIdAndStatus(10L, ReservationStatus.PENDING))
                .thenReturn(0L);
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        ReservationResponse response = reservationService.create(request);

        assertThat(response).isNotNull();
        assertThat(response.getBookTitle()).isEqualTo("Clean Code");
        assertThat(response.getStatus()).isEqualTo(ReservationStatus.PENDING);
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void create_userWithActivePenalty_throwsBusinessException() {
        user.setPenaltyUntil(LocalDate.now().plusDays(5));

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("penalización activa");
    }

    @Test
    void create_bookAvailable_throwsBusinessException() {
        book.setAvailableCopies(2);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disponible");
    }

    @Test
    void create_duplicateReservation_throwsBusinessException() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, ReservationStatus.PENDING))
                .thenReturn(true);

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reserva activa");
    }

    @Test
    void create_queueFull_throwsBusinessException() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.of(book));
        when(reservationRepository.existsByUserIdAndBookIdAndStatus(1L, 10L, ReservationStatus.PENDING))
                .thenReturn(false);
        when(reservationRepository.countByBookIdAndStatus(10L, ReservationStatus.PENDING))
                .thenReturn(3L);

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cola de espera");
    }

    @Test
    void create_userNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_bookNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(bookRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    // ── cancel ──────────────────────────────────────────────────────────────

    @Test
    void cancel_success_setsStatusCancelled() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);
        when(reservationRepository.findByBookIdAndStatusOrderByQueuePositionAsc(10L, ReservationStatus.PENDING))
                .thenReturn(List.of());

        ReservationResponse response = reservationService.cancel(100L);

        assertThat(response).isNotNull();
        verify(reservationRepository).save(argThat(r -> r.getStatus() == ReservationStatus.CANCELLED));
    }

    @Test
    void cancel_differentUser_throwsForbiddenException() {
        User otherUser = User.builder().id(99L).email("other@test.com").build();
        reservation.setUser(otherUser);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(100L))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void cancel_nonPendingReservation_throwsBusinessException() {
        reservation.setStatus(ReservationStatus.FULFILLED);

        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(100L)).thenReturn(Optional.of(reservation));

        assertThatThrownBy(() -> reservationService.cancel(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pendientes");
    }

    @Test
    void cancel_reservationNotFound_throwsResourceNotFoundException() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findById(100L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reservationService.cancel(100L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
    // ── getMyReservations ───────────────────────────────────────────────────

    @Test
    void getMyReservations_returnsUserReservations() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findByUserId(1L)).thenReturn(List.of(reservation));

        List<ReservationResponse> result = reservationService.getMyReservations();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBookTitle()).isEqualTo("Clean Code");
    }

    @Test
    void getMyReservations_noReservations_returnsEmptyList() {
        when(userRepository.findByEmail("user@test.com")).thenReturn(Optional.of(user));
        when(reservationRepository.findByUserId(1L)).thenReturn(List.of());

        List<ReservationResponse> result = reservationService.getMyReservations();

        assertThat(result).isEmpty();
    }
    // ── notifyNextInQueue ───────────────────────────────────────────────────

    @Test
    void notifyNextInQueue_withQueue_setsExpiryDate() {
        when(reservationRepository.findByBookIdAndStatusOrderByQueuePositionAsc(10L, ReservationStatus.PENDING))
                .thenReturn(List.of(reservation));
        when(reservationRepository.save(any(Reservation.class))).thenReturn(reservation);

        reservationService.notifyNextInQueue(10L);
                 
        verify(reservationRepository).save(argThat(r ->
                r.getExpiryDate().equals(LocalDate.now().plusDays(3))
        ));
    }

    @Test
    void notifyNextInQueue_emptyQueue_doesNothing() {
        when(reservationRepository.findByBookIdAndStatusOrderByQueuePositionAsc(10L, ReservationStatus.PENDING))
                .thenReturn(List.of());

        reservationService.notifyNextInQueue(10L);

        verify(reservationRepository, never()).save(any());
    }
    // ── getAllReservations ──────────────────────────────────────────────────

    @Test
    void getAllReservations_returnsAllReservations() {
        when(reservationRepository.findAll()).thenReturn(List.of(reservation));

        List<ReservationResponse> result = reservationService.getAllReservations();

        assertThat(result).hasSize(1);
 
       assertThat(result.get(0).getUserName()).isEqualTo("Test User");
    }
}