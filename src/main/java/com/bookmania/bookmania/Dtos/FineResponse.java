package com.bookmania.bookmania.Dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class FineResponse {
    private Long id;
    private Long loanId;
    private String bookTitle;
    private Integer daysOverdue;
    private Integer penaltyDays;
    private LocalDate penaltyUntil;
    private Long penaltyDaysRemaining; 
    private LocalDateTime createdAt;
}