package myex.shopping.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.cookie")
public record AuthCookieProperties(
        @NotBlank String accessTokenName,
        @NotBlank String refreshTokenName,
        @NotBlank String accessTokenPath,
        @NotBlank String refreshTokenPath,
        String domain,
        @NotBlank String sameSite,
        boolean secure
) {
}
