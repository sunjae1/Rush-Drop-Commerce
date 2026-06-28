package myex.shopping.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myex.shopping.dto.authdto.AuthTokens;
import myex.shopping.dto.userdto.CookieLoginResponse;
import myex.shopping.dto.userdto.LoginRequestDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.exception.JwtAuthenticationException;
import myex.shopping.service.AuthCookieService;
import myex.shopping.service.RefreshTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api")
@Tag(name = "Auth", description = "JWT 인증 API")
public class ApiAuthController {

    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final AuthCookieService authCookieService;

    @Operation(summary = "쿠키 로그인", description = "이메일/비밀번호 인증 후 HttpOnly Cookie 기반 Access/Refresh 토큰을 발급합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "로그인 실패")
    })
    @PostMapping("/login")
    public ResponseEntity<CookieLoginResponse> login(@Valid @RequestBody LoginRequestDto loginRequestDto,
                                                     HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequestDto.getEmail(), loginRequestDto.getPassword())
        );
        PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();
        AuthTokens authTokens = refreshTokenService.issue(principalDetails.getUser());
        authCookieService.addAuthCookies(response, authTokens);

        CookieLoginResponse loginResponse = new CookieLoginResponse(
                authTokens.accessTokenExpiresInSeconds(),
                new UserDto(principalDetails.getUser())
        );

        return ResponseEntity.ok(loginResponse);
    }

    @Operation(summary = "Access Token 재발급", description = "Refresh Token Rotation으로 새로운 Access/Refresh 토큰 쿠키를 발급합니다.", responses = {
            @ApiResponse(responseCode = "204", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰 검증 실패")
    })
    @PostMapping("/auth/refresh")
    public ResponseEntity<Void> refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = authCookieService.resolveRefreshToken(request)
                .orElseThrow(() -> new JwtAuthenticationException("REFRESH_TOKEN_REQUIRED", "리프레시 토큰이 필요합니다."));

        AuthTokens authTokens = refreshTokenService.rotate(refreshToken);
        authCookieService.addAuthCookies(response, authTokens);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "쿠키 로그아웃", description = "Refresh Token을 Redis에서 삭제하고 인증 쿠키를 제거합니다.", responses = {
            @ApiResponse(responseCode = "204", description = "로그아웃 처리 완료")
    })
    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        authCookieService.resolveRefreshToken(request).ifPresent(refreshTokenService::delete);
        authCookieService.clearAuthCookies(response);
        return ResponseEntity.noContent().build();
    }
}
