package myex.shopping.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import jakarta.servlet.http.Cookie;
import myex.shopping.domain.Cart;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.dto.userdto.UserEditDto;
import myex.shopping.form.RegisterForm;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.UserRepository;
import myex.shopping.service.CartService;
import myex.shopping.service.UserService;
import myex.shopping.support.RedisBackedSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiUserControllerTest extends RedisBackedSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private EntityManager em;

    private User testUser;
    private PrincipalDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User("test@example.com", "테스트유저", "password123");
        userService.save(testUser);
        testUserDetails = new PrincipalDetails(testUser);
    }

    @Test
    @WithMockUser
    @DisplayName("회원가입 API 테스트: POST /api/register")
    void registerUser_shouldCreateUser() throws Exception {
        RegisterForm registerForm = new RegisterForm();
        registerForm.setEmail("newuser@example.com");
        registerForm.setName("새로운유저");
        registerForm.setPassword("newpassword");

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerForm))
                .characterEncoding("UTF-8"))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(jsonPath("$.email", is("newuser@example.com")))
                .andExpect(jsonPath("$.name", is("새로운유저")));

        User foundUser = userRepository.findByEmail("newuser@example.com").orElseThrow();
        assertThat(foundUser).isNotNull();
    }

    @Test
    @WithMockUser
    @DisplayName("회원가입 이메일은 소문자로 정규화되어 저장된다")
    void registerUser_shouldNormalizeEmail() throws Exception {
        RegisterForm registerForm = new RegisterForm();
        registerForm.setEmail(" NewUser@Example.com ");
        registerForm.setName("새로운유저");
        registerForm.setPassword("newpassword");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerForm))
                        .characterEncoding("UTF-8"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("newuser@example.com")));

        User foundUser = userRepository.findByEmail("newuser@example.com").orElseThrow();
        assertThat(foundUser.getEmail()).isEqualTo("newuser@example.com");
    }

    @Test
    @DisplayName("로그인 API 성공 테스트: POST /api/login")
    void login_shouldSucceed_withValidCredentials() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\"}"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessTokenExpiresInSeconds", is(300)))
                .andExpect(jsonPath("$.user.email", is(testUser.getEmail())))
                .andReturn();

        Cookie accessCookie = extractCookie(loginResult, "ACCESS_TOKEN");
        Cookie refreshCookie = extractCookie(loginResult, "REFRESH_TOKEN");
        assertThat(accessCookie.getValue()).isNotBlank();
        assertThat(refreshCookie.getValue()).isNotBlank();
        assertThat(loginResult.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .anyMatch(header -> header.contains("HttpOnly"))
                .anyMatch(header -> header.contains("SameSite=Lax"));
    }

    @Test
    @DisplayName("로그인 API 실패 테스트: POST /api/login")
    void login_shouldFail_withInvalidCredentials() throws Exception {
        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"test@example.com\",\"password\":\"wrongpassword\"}"))
                .andDo(print())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("LOGIN_FAILED")));
    }

    @Test
    @WithMockUser
    @DisplayName("전체 회원 조회 API 테스트: GET /api/users")
    void getAllUsers_shouldReturnUserList() throws Exception {
        userService.save(new User("user2@example.com", "유저2", "pass"));

        mockMvc.perform(get("/api/users"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    @DisplayName("회원 정보 수정 API 테스트: PUT /api/users")
    void updateUser_shouldUpdateUserInfo() throws Exception {
        UserEditDto userEditDto = new UserEditDto();
        userEditDto.setName("수정된이름");
        userEditDto.setEmail("updated@example.com");

        mockMvc.perform(put("/api/users")
                .with(user(testUserDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(userEditDto))
                .characterEncoding("UTF-8"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().encoding("UTF-8"))
                .andExpect(jsonPath("$.name", is("수정된이름")))
                .andExpect(jsonPath("$.email", is("updated@example.com")));
    }

    @Test
    @DisplayName("회원 탈퇴 API 테스트: DELETE /api/users")
    void deleteUser_shouldDeactivateUser() throws Exception {
        Long userId = testUser.getId();
        String originalEmail = testUser.getEmail();

        mockMvc.perform(delete("/api/users")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isNoContent());

        User deletedUser = userRepository.findById(userId).orElseThrow();
        assertThat(deletedUser.isActive()).isFalse();
        assertThat(deletedUser.getEmail()).isNotEqualTo(originalEmail);
        assertThat(deletedUser.getEmail()).startsWith("deleted__" + userId + "__");
        assertThat(deletedUser.getEmail()).endsWith("@deleted.local");
    }

    @Test
    @DisplayName("탈퇴 후에는 같은 이메일로 다시 가입할 수 있다")
    void registerUser_shouldAllowReRegisterAfterDeletion() throws Exception {
        mockMvc.perform(delete("/api/users")
                        .with(user(testUserDetails)))
                .andExpect(status().isNoContent());

        RegisterForm registerForm = new RegisterForm();
        registerForm.setEmail("TEST@example.com");
        registerForm.setName("재가입유저");
        registerForm.setPassword("newpassword");

        mockMvc.perform(post("/api/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerForm))
                        .characterEncoding("UTF-8"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email", is("test@example.com")))
                .andExpect(jsonPath("$.name", is("재가입유저")));
    }

    @Test
    @DisplayName("마이페이지 API는 장바구니 상품 이미지에 Pre-signed URL을 반환한다")
    void myPage_shouldReturnPresignedUrlsForCartItems() throws Exception {
        Item item = new Item("마이페이지 상품", 7000, 5, "images/my-page-item.png");
        itemRepository.save(item);

        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(item, 1);
        cartRepository.save(cart);
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/myPage")
                        .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems", hasSize(1)))
                .andExpect(jsonPath("$.cartItems[0].itemName", is(item.getItemName())))
                .andExpect(jsonPath("$.cartItems[0].imageUrl", startsWith("https://")));
    }
}
