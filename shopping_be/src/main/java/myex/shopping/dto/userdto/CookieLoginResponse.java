package myex.shopping.dto.userdto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "쿠키 기반 로그인 응답 DTO")
public record CookieLoginResponse(
        @Schema(description = "액세스 토큰 만료까지 남은 초", example = "300")
        long accessTokenExpiresInSeconds,

        @Schema(description = "로그인한 사용자 정보")
        UserDto user
) {
}
