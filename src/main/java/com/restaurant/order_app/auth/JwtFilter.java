package com.restaurant.order_app.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro que intercepta cada request para validar el JWT del header Authorization.
 * Si el token es válido, carga el usuario y su rol en el SecurityContext.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        // Intentar parsear el token. Si falla, se marca el error en el request
        // para que AuthEntryPoint lo lea y devuelva el mensaje correcto al cliente.
        Claims claims;
        try {
            claims = jwtUtil.extractAllClaims(token);
        } catch (ExpiredJwtException e) {
            request.setAttribute("jwt_error", "El token ha expirado");
            chain.doFilter(request, response);
            return;
        } catch (Exception e) {
            request.setAttribute("jwt_error", "Token inválido");
            chain.doFilter(request, response);
            return;
        }
        String subject = claims.getSubject();
        String rol = (String) claims.get("rol");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                subject,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + rol))
        );
        // Guardamos los claims como details para que los servicios puedan leerlos (ej: mesaId del cliente).
        auth.setDetails(claims);
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }
}
