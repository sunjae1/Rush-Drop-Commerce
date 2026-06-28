package myex.shopping;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import myex.shopping.domain.*;
import myex.shopping.dto.userdto.LoginRequestDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.form.CartForm;
import myex.shopping.form.PostForm;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.OrderRepository;
import myex.shopping.repository.UserRepository;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import myex.shopping.service.ImageService;
import myex.shopping.support.RedisBackedSpringBootTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class IntegrationTest extends RedisBackedSpringBootTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JpaCategoryRepository categoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private ImageService imageService;

    @BeforeEach
    void setupUsersForApiLoginTests() {
        if (userRepository.findByEmail("apiuser@example.com").isEmpty()) {
            User user1 = new User("apiuser@example.com", "Api User", passwordEncoder.encode("password"));
            user1.setActive(true);
            userRepository.save(user1);
        }
        if (userRepository.findByEmail("apiuser2@example.com").isEmpty()) {
            User user2 = new User("apiuser2@example.com", "Api User 2", passwordEncoder.encode("password"));
            user2.setActive(true);
            userRepository.save(user2);
        }
    }

    @Test
    @DisplayName("사용자 전체 시나리오 통합 테스트: 회원가입 -> 로그인 -> 상품 추가 -> 장바구니 담기 -> 주문 -> 게시글 작성")
    void fullUserJourneyTest() throws Exception {
        when(imageService.storeFile(any())).thenReturn("images/fake-image.jpg");
        when(imageService.resolveImageUrls(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(myex.shopping.dto.itemdto.ItemDto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(myex.shopping.dto.itemdto.ItemDtoDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(myex.shopping.dto.itemdto.ItemEditDto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.generatePresignedUrl(any())).thenReturn("https://fake-presigned-url.com/image.jpg");

        // 1. 회원가입 (CSRF 토큰 필요)
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("name", "Test User")
                        .param("email", "testuser@example.com")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        // 2. 로그인 (폼 로그인 → SecurityContext에 PrincipalDetails 저장)
        mockMvc.perform(post("/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("email", "testuser@example.com")
                        .param("password", "password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        // 로그인 후 사용자 조회하여 PrincipalDetails 생성 (아이템 추가를 위해 ADMIN 권한 강제 부여)
        User loggedInUser = userRepository.findByEmail("testuser@example.com").orElseThrow();
        loggedInUser.setRole(Role.ADMIN);
        userRepository.save(loggedInUser);
        PrincipalDetails principalDetails = new PrincipalDetails(loggedInUser);

        // 웹 컨트롤러용 세션 생성 (session.getAttribute("loginUser") 사용하는 웹 컨트롤러 대응)
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUser", loggedInUser);

        Category category = new Category();
        category.setName("테스트 카테고리");
        categoryRepository.save(category);

        // 3. 상품 추가 (웹 UI 통해)
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "test-image.jpg", "image/jpeg",
                "image_content".getBytes());

        MvcResult itemAddResult = mockMvc.perform(multipart("/items/add")
                        .file(imageFile)
                        .with(user(principalDetails))
                        .with(csrf())
                        .session(session)
                        .param("itemName", "Test Item")
                        .param("price", "10000")
                        .param("quantity", "100")
                        .param("categoryId", category.getId().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        String redirectedUrl = itemAddResult.getResponse().getRedirectedUrl();
        assertThat(redirectedUrl).isNotNull();
        long itemId = Long.parseLong(redirectedUrl.substring(redirectedUrl.lastIndexOf('/') + 1));

        // 3.1 추가된 상품 조회하여 확인
        mockMvc.perform(get("/items/{itemId}", itemId)
                        .with(user(principalDetails)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test Item")))
                .andExpect(content().string(containsString(category.getName())));

        // 3.2 CSR용 API 로그인 후 HttpOnly 쿠키 발급
        LoginRequestDto apiLoginRequest = new LoginRequestDto();
        apiLoginRequest.setEmail("testuser@example.com");
        apiLoginRequest.setPassword("password");

        MvcResult apiLoginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(apiLoginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessTokenExpiresInSeconds").value(300))
                .andExpect(jsonPath("$.user.email").value("testuser@example.com"))
                .andReturn();

        assertThat(findSetCookieHeader(apiLoginResult, "ACCESS_TOKEN")).contains("HttpOnly");
        assertThat(findSetCookieHeader(apiLoginResult, "REFRESH_TOKEN")).contains("HttpOnly");
        Cookie accessCookie = extractCookie(apiLoginResult, "ACCESS_TOKEN");

        // 4. 장바구니에 상품 담기 (API 엔드포인트)
        CartForm cartForm = new CartForm();
        cartForm.setId(itemId);
        cartForm.setQuantity(1);

        mockMvc.perform(post("/api/cart/items/{itemId}", itemId)
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cartForm)))
                .andDo(print())
                .andExpect(status().isOk());

        // 5. 주문하기 (웹 UI - session 사용)
        mockMvc.perform(post("/items/order")
                        .with(user(principalDetails))
                        .with(csrf())
                        .session(session))
                .andExpect(status().is3xxRedirection());

        // 5.1 마이페이지에서 주문 내역 확인
        mockMvc.perform(get("/mypage")
                        .with(user(principalDetails))
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Test Item")));

        // 6. 게시글 작성 (API 엔드포인트)
        PostForm postForm = new PostForm();
        postForm.setTitle("New Post Title");
        postForm.setContent("This is the content of the new post.");

        mockMvc.perform(post("/api/posts")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(postForm)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("New Post Title"));
    }

    @Test
    @DisplayName("보안 테스트: 일반 사용자는 상품을 추가할 수 없다 (403 Forbidden)")
    void unauthorizedItemCreationTest() throws Exception {
        when(imageService.storeFile(any())).thenReturn("/fake/path/unauthorized.jpg");

        mockMvc.perform(multipart("/api/items")
                        .with(user("normal-user").roles("USER"))
                        .param("itemName", "Unauthorized Item")
                        .param("price", "5000")
                        .param("quantity", "50"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("주문 취소 실패: 다른 사용자의 주문을 취소할 수 없다")
    void cancelOrder_Fails_WhenUserIsNotOwner() throws Exception {
        User userA = new User("userA@example.com", "User A", "password");
        userRepository.save(userA);
        Item item = new Item("Some Item", 100, 10, "path");
        itemRepository.save(item);
        Order order = new Order(userA);
        order.addOrderItem(new OrderItem(item, item.getPrice(), 1));
        orderRepository.save(order);

        User userB = new User("userB@example.com", "User B", "password");
        userRepository.save(userB);
        PrincipalDetails userBDetails = new PrincipalDetails(userB);

        // 웹 컨트롤러용 세션
        MockHttpSession userBSession = new MockHttpSession();
        userBSession.setAttribute("loginUser", userB);

        // User B가 User A의 주문 취소 시도
        mockMvc.perform(post("/items/{id}/cancel", order.getId())
                        .with(user(userBDetails))
                        .with(csrf())
                        .session(userBSession))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("주문 취소 성공: 사용자가 자신의 주문을 취소한다")
    void cancelOrder_Succeeds_WhenUserIsOwner() throws Exception {
        User userA = new User("userA@example.com", "User A", "password");
        userRepository.save(userA);
        Item item = new Item("Some Item", 100, 10, "path");
        itemRepository.save(item);
        Order order = new Order(userA);
        order.addOrderItem(new OrderItem(item, item.getPrice(), 1));
        orderRepository.save(order);

        PrincipalDetails userADetails = new PrincipalDetails(userA);

        // 웹 컨트롤러용 세션
        MockHttpSession userASession = new MockHttpSession();
        userASession.setAttribute("loginUser", userA);

        // User A가 자신의 주문 취소 시도
        mockMvc.perform(post("/items/{id}/cancel", order.getId())
                        .with(user(userADetails))
                        .with(csrf())
                        .session(userASession))
                .andExpect(status().is3xxRedirection());

        Order cancelledOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }

    @Test
    @DisplayName("API 로그인 성공 테스트")
    void apiLogin_Success() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("apiuser@example.com");
        loginRequest.setPassword("password");

        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.accessTokenExpiresInSeconds").value(300))
                .andExpect(jsonPath("$.user.email").value("apiuser@example.com"))
                .andExpect(jsonPath("$.user.name").value("Api User"))
                .andReturn();

        assertThat(loginResult.getResponse().getHeaders("Set-Cookie"))
                .anyMatch(header -> header.startsWith("ACCESS_TOKEN="))
                .anyMatch(header -> header.startsWith("REFRESH_TOKEN="));
    }

    @Test
    @DisplayName("API 로그인 실패 테스트: 잘못된 비밀번호")
    void apiLogin_Fails_WithWrongPassword() throws Exception {
        LoginRequestDto loginRequest = new LoginRequestDto();
        loginRequest.setEmail("apiuser2@example.com");
        loginRequest.setPassword("wrongpassword");

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }
}
