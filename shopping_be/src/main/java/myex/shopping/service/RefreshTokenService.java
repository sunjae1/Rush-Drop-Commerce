package myex.shopping.service;

import lombok.RequiredArgsConstructor;
import myex.shopping.config.JwtProperties;
import myex.shopping.dto.authdto.AuthTokens;
import myex.shopping.dto.authdto.RefreshTokenSession;
import myex.shopping.domain.User;
import myex.shopping.exception.JwtAuthenticationException;
import myex.shopping.repository.RefreshTokenStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenStore refreshTokenStore;
    private final JwtTokenService jwtTokenService;
    private final JwtProperties jwtProperties;

    public AuthTokens issue(User user) {
        String refreshToken = generateRefreshToken();
        RefreshTokenSession session = createSession(user);
        refreshTokenStore.save(refreshToken, session, jwtProperties.refreshTokenExpiration());

        return new AuthTokens(
                jwtTokenService.createAccessToken(user),
                refreshToken,
                jwtProperties.accessTokenExpiration().toSeconds(),
                jwtProperties.refreshTokenExpiration().toSeconds()
        );
    }

    public AuthTokens rotate(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new JwtAuthenticationException("REFRESH_TOKEN_REQUIRED", "리프레시 토큰이 필요합니다.");
        }

        RefreshTokenSession session = refreshTokenStore.consume(refreshToken)
                .orElseThrow(() -> new JwtAuthenticationException("INVALID_REFRESH_TOKEN", "유효하지 않은 리프레시 토큰입니다."));

        User user = session.toUser();
        String newRefreshToken = generateRefreshToken();
        refreshTokenStore.save(newRefreshToken, createSession(user), jwtProperties.refreshTokenExpiration());

        return new AuthTokens(
                jwtTokenService.createAccessToken(user),
                newRefreshToken,
                jwtProperties.accessTokenExpiration().toSeconds(),
                jwtProperties.refreshTokenExpiration().toSeconds()
        );
    }

    public void delete(String refreshToken) {
        if (StringUtils.hasText(refreshToken)) {
            refreshTokenStore.delete(refreshToken);
        }
    }

    private RefreshTokenSession createSession(User user) {
        return new RefreshTokenSession(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name(),
                UUID.randomUUID().toString()
        );
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[48];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
