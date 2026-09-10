package com.bookmania.bookmania.Controller;

import com.bookmania.bookmania.Dtos.ChangePasswordRequest;
import com.bookmania.bookmania.Services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Not under /api/auth: that prefix is permitAll in SecurityConfig for the
// (necessarily anonymous) register/login endpoints, and this one requires an
// authenticated user - it relies on the default "anyRequest().authenticated()"
// rule instead.
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    @PutMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}
