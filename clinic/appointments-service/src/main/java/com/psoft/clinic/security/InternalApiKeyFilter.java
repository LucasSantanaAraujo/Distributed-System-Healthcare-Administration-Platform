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
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String headerKey = request.getHeader("X-INTERNAL-KEY");

        // Só aplica verificação aos endpoints internos
        if (uri.startsWith("/appointments/internal")) {
            System.out.println("🔍 [InternalApiKeyFilter] Executado para: " + uri);

            if (headerKey == null || !headerKey.equals(internalApiKey)) {
                System.out.println("🚫 [InternalApiKeyFilter] Chave interna inválida!");
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.getWriter().write("Acesso não autorizado (chave interna inválida)");
                return;
            }

            System.out.println("✅ [InternalApiKeyFilter] Chave válida. Acesso permitido.");
        }

        filterChain.doFilter(request, response);
    }
}
