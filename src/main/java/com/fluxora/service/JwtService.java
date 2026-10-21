package com.fluxora.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey;

    private final long expirationMs;

    private final Clock clock;

    @Autowired
    public JwtService(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration-ms}") long expirationMs){
        this(secret, expirationMs, Clock.systemUTC());
    }
    JwtService (String secret, long expirationMs, Clock clock){
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.clock = clock;
    }
    public String generateToken(String email){
        Date now = Date.from(clock.instant());
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder().subject(email).issuedAt(now).expiration(expiration).signWith(signingKey).compact();
    }
    public String extractEmail(String token){
        return etractClaims(token).getSubject();
    }
    public boolean isTokenValid(String token){
        try {
            etractClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException exception){
            return false;
        }
    }
    private Claims etractClaims(String token){
        return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
    }
}
