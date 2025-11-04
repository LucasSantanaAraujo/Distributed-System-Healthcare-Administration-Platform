package com.psoft.clinic.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        System.out.println("🧩 [JwtAuthFilter] Executado para: " + uri);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            System.out.println("⚠️ [JwtAuthFilter] Nenhum token JWT encontrado.");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            System.out.println("🚫 [JwtAuthFilter] Token inválido!");
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtUtil.getEmailFromToken(token);
        String role = jwtUtil.getRoleFromToken(token); // 👈 Este método deve estar no JwtUtil

        System.out.println("✅ [JwtAuthFilter] Token válido: " + email + " | Role: " + role);

        if (role == null) {
            System.out.println("⚠️ [JwtAuthFilter] Nenhum role encontrado no token!");
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ Cria a autoridade correta para o Spring Security
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));

        var auth = new UsernamePasswordAuthenticationToken(
                new User(email, "", authorities),
                null,
                authorities
        );

        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
