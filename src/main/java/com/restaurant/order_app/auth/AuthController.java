package com.restaurant.order_app.auth;

import com.restaurant.order_app.auth.dto.AuthResponse;
import com.restaurant.order_app.auth.dto.ClienteAuthRequest;
import com.restaurant.order_app.auth.dto.StaffAuthRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Expone los endpoints de autenticación para clientes y staff. */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** POST /auth/cliente — login por QR, retorna JWT de 6h con mesaId. */
    @PostMapping("/cliente")
    public ResponseEntity<AuthResponse> loginCliente(@Valid @RequestBody ClienteAuthRequest request) {
        return ResponseEntity.ok(authService.loginCliente(request));
    }

    /** POST /auth/login — login con credenciales, retorna JWT con rol del staff. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginStaff(@Valid @RequestBody StaffAuthRequest request) {
        return ResponseEntity.ok(authService.loginStaff(request));
    }
}
