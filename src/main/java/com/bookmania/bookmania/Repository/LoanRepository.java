package com.bookmania.bookmania.Repository;

import com.bookmania.bookmania.Entity.Loan;
import com.bookmania.bookmania.Enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByUserId(Long userId);
    List<Loan> findByUserIdAndStatus(Long userId, LoanStatus status);
    boolean existsByUserIdAndBookIdAndStatus(Long userId, Long bookId, LoanStatus status);
}