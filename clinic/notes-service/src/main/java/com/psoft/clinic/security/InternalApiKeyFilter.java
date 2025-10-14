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
        System.out.println("🔍 InternalApiKeyFilter executado para: " + request.getRequestURI());


        String headerKey = request.getHeader("X-INTERNAL-KEY");

        // Apenas protege endpoints internos
        if (request.getRequestURI().startsWith("/patients")) {
            if (headerKey == null || !headerKey.equals(internalApiKey)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("Acesso não autorizado (chave interna inválida)");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

