package myex.shopping.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import myex.shopping.config.AuthCookieProperties;
import myex.shopping.config.JwtProperties;
import myex.shopping.dto.authdto.AuthTokens;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthCookieService {

    private final AuthCookieProperties authCookieProperties;
    private final JwtProperties jwtProperties;

    public Optional<String> resolveAccessToken(HttpServletRequest request) {
        return resolveCookieValue(request, authCookieProperties.accessTokenName());
    }

    public Optional<String> resolveRefreshToken(HttpServletRequest request) {
        return resolveCookieValue(request, authCookieProperties.refreshTokenName());
    }

    public void addAuthCookies(HttpServletResponse response, AuthTokens authTokens) {
        addCookie(response,
                authCookieProperties.accessTokenName(),
                authTokens.accessToken(),
                authCookieProperties.accessTokenPath(),
                jwtProperties.accessTokenExpiration());
        addCookie(response,
                authCookieProperties.refreshTokenName(),
                authTokens.refreshToken(),
                authCookieProperties.refreshTokenPath(),
                jwtProperties.refreshTokenExpiration());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, authCookieProperties.accessTokenName(), authCookieProperties.accessTokenPath());
        clearCookie(response, authCookieProperties.refreshTokenName(), authCookieProperties.refreshTokenPath());
    }

    private Optional<String> resolveCookieValue(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }

        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private void addCookie(HttpServletResponse response, String name, String value, String path, Duration maxAge) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path(path)
                .maxAge(maxAge);

        if (StringUtils.hasText(authCookieProperties.domain())) {
            builder.domain(authCookieProperties.domain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    private void clearCookie(HttpServletResponse response, String name, String path) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, "")
                .httpOnly(true)
                .secure(authCookieProperties.secure())
                .sameSite(authCookieProperties.sameSite())
                .path(path)
                .maxAge(Duration.ZERO);

        if (StringUtils.hasText(authCookieProperties.domain())) {
            builder.domain(authCookieProperties.domain());
        }

        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }
}
