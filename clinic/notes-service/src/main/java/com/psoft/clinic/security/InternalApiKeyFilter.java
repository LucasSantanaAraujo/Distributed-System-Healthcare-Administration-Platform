package com.psoft.clinic.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        System.out.println("🔍 [InternalApiKeyFilter] Executado para: " + uri);

        // 🔒 Apenas verifica endpoints internos (records, physicians, patients, etc.)
        boolean isInternalEndpoint = uri.startsWith("/records/internal")
                || uri.startsWith("/physicians/internal")
                || uri.startsWith("/patients/internal")
                || uri.startsWith("/appointments/internal");

        if (isInternalEndpoint) {
            String headerKey = request.getHeader("X-INTERNAL-KEY");

            System.out.println("🧩 [InternalApiKeyFilter] Header recebido: " + headerKey);
            System.out.println("🧩 [InternalApiKeyFilter] Chave esperada: " + internalApiKey);

            if (headerKey == null) {
                System.out.println("🚫 [InternalApiKeyFilter] Nenhuma X-INTERNAL-KEY recebida.");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Acesso negado: header X-INTERNAL-KEY ausente");
                return;
            }

            if (!headerKey.equals(internalApiKey)) {
                System.out.println("🚫 [InternalApiKeyFilter] Chave inválida! Acesso negado.");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Acesso negado: X-INTERNAL-KEY incorreta");
                return;
            }

            System.out.println("✅ [InternalApiKeyFilter] Chave válida, acesso permitido ao endpoint interno.");
        }

        filterChain.doFilter(request, response);
    }
}
