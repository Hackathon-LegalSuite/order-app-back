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

    /** POST /auth/cliente/{mesaId} — login por QR, valida que el código ingresado corresponda a la mesa del path. */
    @PostMapping("/cliente/{mesaId}")
    public ResponseEntity<AuthResponse> loginCliente(@PathVariable Long mesaId,
                                                     @Valid @RequestBody ClienteAuthRequest request) {
        return ResponseEntity.ok(authService.loginCliente(mesaId, request));
    }

    /** POST /auth/login — login con credenciales, retorna JWT con rol del staff. */
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginStaff(@Valid @RequestBody StaffAuthRequest request) {
        return ResponseEntity.ok(authService.loginStaff(request));
    }
}
