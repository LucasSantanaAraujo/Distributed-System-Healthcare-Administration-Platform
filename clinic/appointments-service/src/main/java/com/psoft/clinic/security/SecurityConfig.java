package com.psoft.clinic.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalApiKeyFilter internalApiKeyFilter; // 👈 adiciona este filtro

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ❌ Desativa CSRF
                .csrf(csrf -> csrf.disable())

                // 🚫 Sem sessões
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ Regras de autorização
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/h2-console/**",
                                "/register/**",
                                "/login",
                                "/error",
                                "/actuator/**",
                                "/appointments/internal/**" // 👈 importante permitir o endpoint interno
                        ).permitAll()
                        .anyRequest().authenticated()
                )

                // 🔑 Regista o filtro de chave interna antes do JWT
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)

                // 🧩 Filtro JWT continua a seguir
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                // 🪟 Permite o uso do H2 console
                .headers(headers -> headers.frameOptions(frame -> frame.disable()));

        return http.build();
    }
}
