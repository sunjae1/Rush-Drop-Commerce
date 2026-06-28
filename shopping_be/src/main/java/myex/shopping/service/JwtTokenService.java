package myex.shopping.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import myex.shopping.config.JwtProperties;
import myex.shopping.domain.Role;
import myex.shopping.domain.User;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.exception.JwtAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtProperties jwtProperties;

    private SecretKey signingKey;

    @PostConstruct
    void init() {
        byte[] keyBytes = jwtProperties.accessSecret().getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException("JWT access secret must be at least 32 bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createAccessToken(PrincipalDetails principalDetails) {
        return createAccessToken(principalDetails.getUser());
    }

    public String createAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.accessTokenExpiration());

        return Jwts.builder()
                .subject(user.getId().toString())
                .issuer(jwtProperties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .claim("role", user.getRole().name())
                .signWith(signingKey)
                .compact();
    }

    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        PrincipalDetails principalDetails = new PrincipalDetails(toUser(claims));

        return new UsernamePasswordAuthenticationToken(
                principalDetails,
                token,
                principalDetails.getAuthorities()
        );
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpiration().toSeconds();
    }

    private Claims parseClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new JwtAuthenticationException("TOKEN_EXPIRED", "만료된 JWT 토큰입니다.");
        } catch (JwtException | IllegalArgumentException ex) {
            throw new JwtAuthenticationException("INVALID_TOKEN", "유효하지 않은 JWT 토큰입니다.");
        }
    }

    private Long parseUserId(String subject) {
        try {
            return Long.valueOf(subject);
        } catch (NumberFormatException ex) {
            throw new JwtAuthenticationException("INVALID_TOKEN", "유효하지 않은 JWT 토큰입니다.");
        }
    }

    private User toUser(Claims claims) {
        User user = new User();
        user.setId(parseUserId(claims.getSubject()));
        user.setEmail(claims.get("email", String.class));
        user.setName(claims.get("name", String.class));
        user.setRole(Role.valueOf(claims.get("role", String.class)));
        user.setActive(true);
        return user;
    }
}
