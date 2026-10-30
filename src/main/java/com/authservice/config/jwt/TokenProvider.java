package com.authservice.config.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import com.authservice.domain.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class TokenProvider {

    // HS256 서명 키는 256비트(32바이트) 이상이어야 한다
    private static final int MIN_SECRET_KEY_BYTES = 32;

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Value("${jwt.expiration-hours}")
    private long expirationHours;

    private SecretKey signingKey;

    // 서버 시작 시 비밀키를 검사한다.
    // 비어 있거나 짧은 키로는 서버가 뜨지 않게 해서, 누구나 아는 키로 토큰을 위조하는 일을 막는다.
    @PostConstruct
    void init() {
        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("JWT_SECRET_KEY 환경 변수를 설정해야 합니다.");
        }
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < MIN_SECRET_KEY_BYTES) {
            throw new IllegalStateException("JWT_SECRET_KEY는 32바이트 이상이어야 합니다.");
        }
        signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    // JWT 토큰 생성
    public String generateToken(User user, Duration expiredAt) {
        Date now = new Date();
        return makeToken(new Date(now.getTime() + expiredAt.toMillis()), user);
    }

    // JWT 토큰 생성 메서드
    private String makeToken(Date expiry, User user) {
        Date now = new Date();

        return Jwts.builder()
                .header()
                    .add("typ", "JWT")
                    .and()
                .issuer("auth-service")
                .issuedAt(now)
                .expiration(expiry)
                .subject(user.getEmail())
                .claim("id", user.getId())
                .claim("email", user.getEmail())
                .signWith(getSecretKey(), Jwts.SIG.HS256)
                .compact();
    }

    // 비밀키 (서버 시작 시 한 번 만든 키를 재사용)
    private SecretKey getSecretKey() {
        return signingKey;
    }

    // JWT 토큰 유효성 검증
    public boolean validToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // 토큰 기반으로 인증 정보를 가져오는 메서드
    public Authentication getAuthentication(String token) {
        Claims claims = getClaims(token);
        Set<SimpleGrantedAuthority> authorities = Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"));

        return new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        claims.getSubject(),
                        "",
                        authorities
                ),
                token,
                authorities
        );
    }

    // 토큰 기반으로 유저 ID를 가져오는 메서드
    public Long getUserId(String token) {
        Claims claims = getClaims(token);
        return claims.get("id", Long.class);
    }

    // 토큰에서 이메일을 가져오는 메서드
    public String getUserEmail(String token) {
        Claims claims = getClaims(token);
        return claims.get("email", String.class);
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
