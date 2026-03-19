// ReservationResponse.java
package com.bookmania.bookmania.Dtos;

import com.bookmania.bookmania.Enums.ReservationStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReservationResponse {
    private Long id;
    private String userName;
    private String bookTitle;
    private Integer queuePosition;
    private LocalDate reservationDate;
    private LocalDate expiryDate;
    private ReservationStatus status;
    private LocalDateTime createdAt;
}