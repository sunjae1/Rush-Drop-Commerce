package myex.shopping.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import myex.shopping.domain.User;
import myex.shopping.dto.userdto.LoginRequestDto;
import myex.shopping.service.UserService;
import myex.shopping.support.RedisBackedSpringBootTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "FRONT_END_ORIGIN=http://localhost:5173,https://localhost:5173")
@AutoConfigureMockMvc
class SecurityConfigIntegrationTest extends RedisBackedSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Test
    @DisplayName("비로그인 사용자도 메인 페이지에 접근할 수 있다")
    void mainPageShouldBeAccessibleWithoutLogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("허용된 프론트 origin의 API preflight 요청은 CORS 헤더와 함께 통과한다")
    void apiPreflightShouldReturnCorsHeadersForAllowedOrigin() throws Exception {
        mockMvc.perform(options("/api/items")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("https localhost origin도 API preflight 요청을 허용한다")
    void apiPreflightShouldReturnCorsHeadersForHttpsLocalhostOrigin() throws Exception {
        mockMvc.perform(options("/api/items")
                        .header("Origin", "https://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }

    @Test
    @DisplayName("API 로그인은 HttpOnly 쿠키를 발급하고 보호된 API 접근을 허용한다")
    void apiLoginShouldIssueJwtAndAllowProtectedApiAccess() throws Exception {
        User user = createUser();

        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(user))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessTokenExpiresInSeconds").value(300))
                .andExpect(jsonPath("$.user.email").value(user.getEmail()))
                .andReturn();

        assertThat(findSetCookieHeader(loginResult, "ACCESS_TOKEN"))
                .contains("HttpOnly")
                .contains("Path=/api")
                .contains("SameSite=Lax");
        assertThat(findSetCookieHeader(loginResult, "REFRESH_TOKEN"))
                .contains("HttpOnly")
                .contains("Path=/api/auth")
                .contains("SameSite=Lax");

        Cookie accessCookie = extractCookie(loginResult, "ACCESS_TOKEN");

        mockMvc.perform(get("/api")
                        .cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userDto.email").value(user.getEmail()));
    }

    @Test
    @DisplayName("API 로그인은 이메일 대소문자를 정규화하여 인증한다")
    void apiLoginShouldNormalizeEmail() throws Exception {
        User user = createUser();
        LoginRequestDto loginRequestDto = loginRequest(user);
        loginRequestDto.setEmail(" " + user.getEmail().toUpperCase() + " ");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value(user.getEmail()));
    }

    @Test
    @DisplayName("웹 세션이 있어도 API는 JWT 없으면 인증되지 않는다")
    void apiShouldIgnoreWebSessionAndRequireJwt() throws Exception {
        User user = createUser();
        MockHttpSession session = loginWeb(user);

        mockMvc.perform(get("/mypage").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    @Test
    @DisplayName("API 로그아웃은 기존 웹 세션을 무효화하지 않는다")
    void apiLogoutShouldNotInvalidateWebSession() throws Exception {
        User user = createUser();
        MockHttpSession session = loginWeb(user);
        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(user))))
                .andExpect(status().isOk())
                .andReturn();
        Cookie refreshCookie = extractCookie(loginResult, "REFRESH_TOKEN");

        MvcResult logoutResult = mockMvc.perform(post("/api/auth/logout")
                        .session(session)
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andReturn();

        assertThat(logoutResult.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith("ACCESS_TOKEN="))
                .anyMatch(header -> header.startsWith("REFRESH_TOKEN="));

        mockMvc.perform(get("/mypage").session(session))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("리프레시 토큰 재발급은 rotation으로 새 리프레시 쿠키를 발급한다")
    void refreshShouldRotateRefreshToken() throws Exception {
        User user = createUser();
        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(user))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = extractCookie(loginResult, "REFRESH_TOKEN");
        String previousRefreshToken = refreshCookie.getValue();

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie rotatedRefreshCookie = extractCookie(refreshResult, "REFRESH_TOKEN");
        assertThat(rotatedRefreshCookie.getValue()).isNotEqualTo(previousRefreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    @DisplayName("동일한 리프레시 토큰 동시 요청은 정확히 하나만 성공한다")
    void refreshShouldAllowOnlyOneConcurrentRotation() throws Exception {
        User user = createUser();
        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest(user))))
                .andExpect(status().isOk())
                .andReturn();

        Cookie refreshCookie = extractCookie(loginResult, "REFRESH_TOKEN");
        CountDownLatch startSignal = new CountDownLatch(1);

        Callable<Integer> refreshRequest = () -> {
            startSignal.await();
            return mockMvc.perform(post("/api/auth/refresh")
                            .cookie(refreshCookie))
                    .andReturn()
                    .getResponse()
                    .getStatus();
        };

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executorService.submit(refreshRequest);
            Future<Integer> second = executorService.submit(refreshRequest);

            startSignal.countDown();

            List<Integer> statuses = List.of(first.get(), second.get());
            assertThat(statuses).containsExactlyInAnyOrder(204, 401);
        } finally {
            executorService.shutdownNow();
        }
    }

    private User createUser() {
        String unique = UUID.randomUUID().toString();
        User user = new User("jwt-" + unique + "@example.com", "JWT 사용자", "password123!");
        userService.save(user);
        return user;
    }

    private LoginRequestDto loginRequest(User user) {
        LoginRequestDto loginRequestDto = new LoginRequestDto();
        loginRequestDto.setEmail(user.getEmail());
        loginRequestDto.setPassword("password123!");
        return loginRequestDto;
    }

    private MockHttpSession loginWeb(User user) throws Exception {
        MvcResult result = mockMvc.perform(post("/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", user.getEmail())
                        .param("password", "password123!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        return session;
    }
}

