package myex.shopping;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // 내부적으로 @ComponentScan 포함 : 빈 찾아서 등록해줌. : 기본 스캔 경로 : 현재 패키지와 하위 패키지 (shopping 과
						// 그 하위.)
@OpenAPIDefinition(info = @Info(title = "My Shopping Mall", version = "1.0.0", description = "나의 쇼핑몰 API 문서 - Swagger"), servers = {
		@Server(url = "http://localhost:8080", description = "로컬 개발 서버")
})
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT",
		description = "브라우저에서는 HttpOnly 쿠키 기반으로 주로 사용하고, 수동 API 호출/Swagger 테스트용으로 Bearer JWT도 지원")
public class ShoppingApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShoppingApplication.class, args);
	}

}
