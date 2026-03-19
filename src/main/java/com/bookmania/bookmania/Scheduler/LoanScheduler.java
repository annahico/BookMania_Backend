package com.bookmania.bookmania.Scheduler;

import com.bookmania.bookmania.Entity.Loan;
import com.bookmania.bookmania.Enums.LoanStatus;
import com.bookmania.bookmania.Repository.LoanRepository;
import com.bookmania.bookmania.Services.FineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class LoanScheduler {

    private final LoanRepository loanRepository;
    private final FineService fineService;

    // @Scheduled(cron = "0 5 0 * * *") // Ejecutar diariamente a las 00:05
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void markOverdueLoans() {
        List<Loan> activeLoans = loanRepository.findByStatus(LoanStatus.ISSUED);
        int count = 0;

        for (Loan loan : activeLoans) {
            if (LocalDate.now().isAfter(loan.getDueDate())) {
                loan.setStatus(LoanStatus.OVERDUE);
                loanRepository.save(loan);
                fineService.generateFine(loan);
                count++;
            }
        }
        log.info("Scheduler ejecutado: {} préstamos marcados como OVERDUE", count);
    }
}