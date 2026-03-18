package com.bookmania.bookmania.Controller;

import com.bookmania.bookmania.Dtos.LoanRequest;
import com.bookmania.bookmania.Dtos.LoanResponse;
import com.bookmania.bookmania.Services.LoanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
@RequiredArgsConstructor
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    public ResponseEntity<LoanResponse> create(@Valid @RequestBody LoanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.create(request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<LoanResponse>> getMyLoans() {
        return ResponseEntity.ok(loanService.getMyLoans());
    }

    @PutMapping("/{id}/extend")
    public ResponseEntity<LoanResponse> extend(@PathVariable Long id) {
        return ResponseEntity.ok(loanService.extend(id));
    }
}
