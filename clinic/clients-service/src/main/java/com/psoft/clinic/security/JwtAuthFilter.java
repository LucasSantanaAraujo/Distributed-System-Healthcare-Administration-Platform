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

        String authHeader = request.getHeader("Authorization");

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                System.out.println("🎟️ Token recebido: " + token);

                if (jwtUtil.validateToken(token)) {
                    System.out.println("✅ Token válido!");

                    String email = jwtUtil.getEmailFromToken(token);
                    String role = jwtUtil.getRoleFromToken(token);

                    System.out.println("📧 Email extraído: " + email);
                    System.out.println("🧩 Role extraída do token: " + role);

                    String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                    System.out.println("🔐 Autoridade final atribuída: " + authority);

                    List<SimpleGrantedAuthority> authorities =
                            List.of(new SimpleGrantedAuthority(authority));

                    User user = new User(email, "", authorities);

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(user, null, authorities);
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    System.out.println("✅ Utilizador autenticado no SecurityContext.");
                } else {
                    System.out.println("❌ Token inválido!");
                }
            } else {
                System.out.println("⚠️ Nenhum header Authorization encontrado!");
            }
        } catch (Exception e) {
            System.out.println("⚠️ Erro ao processar JWT: " + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
