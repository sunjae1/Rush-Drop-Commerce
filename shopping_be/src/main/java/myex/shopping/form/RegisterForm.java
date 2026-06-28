package myex.shopping.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Schema(description = "회원가입 정보 담는 FORM")
public class RegisterForm {
    @NotEmpty(message = "사용자 이름은 필수 입니다.")
    @Schema(description = "사용자 이름", example = "테스터A")
    private String name;
    @NotEmpty(message = "이메일을 입력해주세요.")
    @Email
    @Schema(description = "사용자 이메일", example = "test@na.com")
    private String email;
    @NotEmpty(message = "비밀번호를 입력해주세요")
    @Size(min = 3, max = 15, message = "비밀번호는 3자 이상 15자 이하 입니다.")
    @Schema(description = "사용자 비밀번호", example = "123AASD!21!!")
    private String password;

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
