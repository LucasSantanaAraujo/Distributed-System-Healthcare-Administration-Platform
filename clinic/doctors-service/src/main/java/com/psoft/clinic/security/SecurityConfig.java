package com.psoft.clinic.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * ⚙️ Configuração de segurança (temporariamente neutra).
 *
 * O doctors-service não gere autenticação diretamente.
 * Este ficheiro pertence ao futuro auth-service e fica aqui
 * apenas para referência e compatibilidade até a divisão final.
 */

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Configuração básica do Spring Security apenas para permitir
     * todos os endpoints (sem autenticação JWT nesta fase).
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll() // 🔓 Tudo permitido temporariamente
                );
        return http.build();
    }

    /**
     * Encoder simples (mantido para compatibilidade, se necessário).
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
