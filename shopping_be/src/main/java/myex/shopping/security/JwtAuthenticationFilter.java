package myex.shopping.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.exception.JwtAuthenticationException;
import myex.shopping.service.AuthCookieService;
import myex.shopping.service.JwtTokenService;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String AUTH_ERROR_CODE_ATTRIBUTE = "authErrorCode";
    public static final String AUTH_ERROR_MESSAGE_ATTRIBUTE = "authErrorMessage";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenService jwtTokenService;
    private final AuthCookieService authCookieService;

    public JwtAuthenticationFilter(JwtTokenService jwtTokenService, AuthCookieService authCookieService) {
        this.jwtTokenService = jwtTokenService;
        this.authCookieService = authCookieService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            Authentication authentication = jwtTokenService.getAuthentication(token);
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            filterChain.doFilter(request, response);
        } catch (JwtAuthenticationException ex) {
            setAuthenticationError(request, ex);
            filterChain.doFilter(request, response);
        } catch (AuthenticationException ex) {
            log.debug("JWT authentication failed", ex);
            setAuthenticationError(request,
                    new JwtAuthenticationException("INVALID_TOKEN", "유효하지 않은 JWT 토큰입니다."));
            filterChain.doFilter(request, response);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        return authCookieService.resolveAccessToken(request)
                .orElseGet(() -> resolveAuthorizationHeaderToken(request));
    }

    private String resolveAuthorizationHeaderToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorizationHeader)) {
            return null;
        }
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            setAuthenticationError(request,
                    new JwtAuthenticationException("INVALID_TOKEN", "Authorization 헤더는 Bearer 토큰 형식이어야 합니다."));
            return null;
        }

        String token = authorizationHeader.substring(BEARER_PREFIX.length()).trim();
        if (!StringUtils.hasText(token)) {
            setAuthenticationError(request,
                    new JwtAuthenticationException("INVALID_TOKEN", "JWT 토큰이 비어 있습니다."));
            return null;
        }
        return token;
    }

    private void setAuthenticationError(HttpServletRequest request, JwtAuthenticationException exception) {
        SecurityContextHolder.clearContext();
        request.setAttribute(AUTH_ERROR_CODE_ATTRIBUTE, exception.getCode());
        request.setAttribute(AUTH_ERROR_MESSAGE_ATTRIBUTE, exception.getMessage());
    }
}
