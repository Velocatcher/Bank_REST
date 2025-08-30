package com.example.bankcards.security;

import com.example.bankcards.entity.Role;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {
    private final Key key;
    private final long expMinutes;

    public JwtService(@Value("${app.security.jwt-secret}") String secret,
                      @Value("${app.security.jwt-exp-min}") long expMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes()); // почему: HS256 требует надёжный ключ
        this.expMinutes = expMinutes;
    }

    public String generateToken(String username, Set<Role> roles) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(expMinutes * 60)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) { return parse(token).getBody().getSubject(); }

    public boolean isValid(String token) {
        try { parse(token); return true; } catch (JwtException ex) { return false; }
    }

    private Jws<Claims> parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
    }

}
