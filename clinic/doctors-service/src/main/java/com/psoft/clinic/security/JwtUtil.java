package com.psoft.clinic.security;

// import com.psoft.clinic.model.User; // ⚠️ Classe removida deste serviço
import io.jsonwebtoken.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Classe de utilitários JWT (autenticação).
 * ⚠️ O doctors-service já não gere autenticação nem tokens JWT.
 * Este ficheiro é deixado comentado para referência futura.
 *
 * Será migrado para o auth-service quando a divisão estiver concluída.
 */

@Component
public class JwtUtil {

    @Value("${app.jwt.secret:dummy-secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration:86400000}") // 1 dia por defeito
    private int jwtExpirationMs;

    // 🔸 Método original dependia de com.psoft.clinic.model.User
    //    Por agora, deixamos desativado.
    /*
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .claim("role", user.getRole().name())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(SignatureAlgorithm.HS512, jwtSecret.getBytes())
                .compact();
    }
    */

    // 🔹 Estes métodos não dependem de User e podem ficar ativos
    public String extractRole(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())
                    .parseClaimsJws(token)
                    .getBody()
                    .get("role", String.class);
        } catch (JwtException e) {
            return null;
        }
    }

    public String getEmailFromToken(String token) {
        try {
            return Jwts.parser()
                    .setSigningKey(jwtSecret.getBytes())
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
        } catch (JwtException e) {
            return null;
        }
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtSecret.getBytes())
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
