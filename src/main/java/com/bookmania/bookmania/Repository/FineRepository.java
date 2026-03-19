package com.bookmania.bookmania.Repository;

import com.bookmania.bookmania.Entity.Fine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FineRepository extends JpaRepository<Fine, Long> {
    Optional<Fine> findByLoanId(Long loanId);
    List<Fine> findByUserId(Long userId);
    boolean existsByLoanIdAndPaidFalse(Long loanId);
    boolean existsByUserIdAndPaidFalse(Long userId);
}