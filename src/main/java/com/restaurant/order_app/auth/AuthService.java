package com.restaurant.order_app.auth;

import com.restaurant.order_app.auth.dto.AuthResponse;
import com.restaurant.order_app.auth.dto.ClienteAuthRequest;
import com.restaurant.order_app.auth.dto.StaffAuthRequest;
import com.restaurant.order_app.mesa.Mesa;
import com.restaurant.order_app.mesa.MesaRepository;
import com.restaurant.order_app.usuario.Usuario;
import com.restaurant.order_app.usuario.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Lógica de autenticación para clientes (por QR) y staff (por credenciales). */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final MesaRepository mesaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Valida que la mesa del path exista y que el código ingresado por el cliente corresponda a esa mesa.
     * Evita que un cliente use el QR de una mesa pero ingrese el código de otra.
     */
    public AuthResponse loginCliente(Long mesaId, ClienteAuthRequest request) {
        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mesa no encontrada"));

        if (!mesa.getCodigoQr().equalsIgnoreCase(request.getCodigoMesa())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código ingresado no corresponde a esta mesa");
        }

        String token = jwtUtil.generateToken(
                request.getNombre(),
                Map.of("mesaId", mesa.getId(), "rol", "CLIENTE", "clienteId", java.util.UUID.randomUUID().toString()),
                6
        );

        return AuthResponse.builder()
                .token(token)
                .rol("CLIENTE")
                .nombre(request.getNombre())
                .mesaId(mesa.getId())
                .expiresIn("6h")
                .build();
    }

    /** Valida username y contraseña (BCrypt) del staff y genera un JWT con su rol. */
    public AuthResponse loginStaff(StaffAuthRequest request) {
        Usuario usuario = usuarioRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas"));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
        }

        String token = jwtUtil.generateToken(
                usuario.getUsername(),
                Map.of("rol", usuario.getRol().name(), "nombre", usuario.getNombre()),
                8
        );

        return AuthResponse.builder()
                .token(token)
                .rol(usuario.getRol().name())
                .nombre(usuario.getNombre())
                .build();
    }
}
