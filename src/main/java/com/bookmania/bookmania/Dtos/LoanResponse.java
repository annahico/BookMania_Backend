package com.bookmania.bookmania.Dtos;

import com.bookmania.bookmania.Enums.LoanStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class LoanResponse {
    private Long id;
    private String userName;
    private String bookTitle;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private int extensionsUsed;
    private LoanStatus status;
}