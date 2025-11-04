package com.psoft.clinic.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final InternalApiKeyFilter internalApiKeyFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // ❌ Desativa CSRF (usamos JWT)
                .csrf(csrf -> csrf.disable())

                // 🚫 Sem sessões (stateless)
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ✅ Permissões
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/h2-console/**",
                                "/actuator/**",
                                "/auth/login",
                                "/auth/register",
                                "/auth/register/patient",
                                "/physicians/internal"
                        ).permitAll()
                        // 👉 o InternalApiKeyFilter lida com o X-INTERNAL-KEY
                        .requestMatchers("/physicians/**").authenticated()
                        .anyRequest().authenticated()
                )

                // 🧱 Permite H2 console
                .headers(headers -> headers.frameOptions(frame -> frame.disable()))

                // 🔑 Adiciona os filtros personalizados
                // 👉 O InternalApiKeyFilter vem ANTES do JwtAuthFilter
                .addFilterBefore(internalApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
