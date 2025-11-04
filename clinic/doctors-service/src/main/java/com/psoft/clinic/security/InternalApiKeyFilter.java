package com.psoft.clinic.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class InternalApiKeyFilter extends OncePerRequestFilter {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();
        String headerKey = request.getHeader("X-INTERNAL-KEY");
        String authHeader = request.getHeader("Authorization");

        System.out.println("🔍 [InternalApiKeyFilter] URI: " + uri);
        System.out.println("   ➤ X-INTERNAL-KEY: " + headerKey);
        System.out.println("   ➤ Authorization: " + authHeader);

        // 👉 Só valida a chave se for uma chamada interna (tiver o header X-INTERNAL-KEY)
        if (headerKey != null) {
            if (!headerKey.equals(internalApiKey)) {
                System.out.println("🚫 [InternalApiKeyFilter] Chave interna inválida!");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\":\"Chave interna inválida.\"}");
                return;
            } else {
                System.out.println("✅ [InternalApiKeyFilter] Chave interna válida — acesso interno permitido.");

                // ✅ Autentica o pedido como "internal-service"
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(
                                "internal-service",
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))
                        );
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        } else {
            // 👉 Se não tiver X-INTERNAL-KEY, deixamos o Spring Security tratar o JWT
            System.out.println("ℹ️ [InternalApiKeyFilter] Sem X-INTERNAL-KEY — pedido com JWT, a ignorar filtro interno.");
        }

        filterChain.doFilter(request, response);
    }
}
