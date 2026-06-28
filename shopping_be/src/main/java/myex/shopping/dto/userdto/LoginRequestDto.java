package myex.shopping.dto.userdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Schema(description = "로그인 요청 DTO")
public class LoginRequestDto {

    @NotBlank(message = "이메일은 필수 입니다.")
    @Schema(description = "사용자 이메일", example = "test@na.com")
    private String email;

    @NotBlank(message = "비밀번호는 필수 입니다.")
    @Schema(description = "사용자 비밀번호", example = "test1231!@")
    private String password;

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
