package com.bookmania.bookmania.Controller;

import com.bookmania.bookmania.Dtos.FineResponse;
import com.bookmania.bookmania.Services.FineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fines")
@RequiredArgsConstructor
public class FineController {

    private final FineService fineService;

    @GetMapping("/my")
    public ResponseEntity<List<FineResponse>> getMyFines() {
        return ResponseEntity.ok(fineService.getMyFines());
    }
}