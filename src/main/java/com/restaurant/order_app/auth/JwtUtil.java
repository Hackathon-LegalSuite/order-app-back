package com.restaurant.order_app.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/** Utilidad para generar y validar tokens JWT usando HMAC-SHA256. */
@Component
public class JwtUtil {

    @Value("${JWT_SECRET}")
    private String secret;

    /** Construye la clave secreta a partir de la variable de entorno JWT_SECRET. */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** Genera un JWT firmado con el subject, claims adicionales y duración en horas. */
    public String generateToken(String subject, Map<String, Object> extraClaims, long hours) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(subject)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + hours * 3600 * 1000))
                .signWith(getKey())
                .compact();
    }

    /** Extrae y retorna todos los claims del token. Lanza excepción si el token es inválido o expiró. */
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Retorna true si el token tiene firma válida y no está expirado. */
    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
