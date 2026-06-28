package myex.shopping.controller.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import myex.shopping.domain.Cart;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.form.CartForm;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.UserRepository;
import myex.shopping.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ApiCartControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
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
    private Item testItem;
    private PrincipalDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User("testuser@example.com", "테스트유저", "password");
        userRepository.save(testUser);

        testItem = new Item("테스트 상품", 10000, 10, "images/test-item.png");
        itemRepository.save(testItem);

        testUserDetails = new PrincipalDetails(testUser);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("장바구니에 상품 추가 API 테스트: POST /api/items/{itemId}/cart")
    void addToCart_shouldAddItem() throws Exception {
        CartForm cartForm = new CartForm();
        cartForm.setId(testItem.getId());
        cartForm.setQuantity(2);

        mockMvc.perform(post("/api/cart/items/{itemId}", testItem.getId())
                .with(user(testUserDetails))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(cartForm)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems", hasSize(1)))
                .andExpect(jsonPath("$.cartItems[0].item.itemName", is(testItem.getItemName())))
                .andExpect(jsonPath("$.cartItems[0].item.imageUrl", startsWith("https://")))
                .andExpect(jsonPath("$.cartItems[0].quantity", is(2)))
                .andExpect(jsonPath("$.allPrice", is(20000)));
    }

    @Test
    @DisplayName("장바구니 조회 API 테스트: GET /api/items/cartAll")
    void getCart_shouldReturnCartContents() throws Exception {
        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(testItem, 3);
        cartRepository.save(cart);
        em.flush();

        mockMvc.perform(get("/api/cart")
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems", hasSize(1)))
                .andExpect(jsonPath("$.cartItems[0].item.itemName", is(testItem.getItemName())))
                .andExpect(jsonPath("$.cartItems[0].item.imageUrl", startsWith("https://")))
                .andExpect(jsonPath("$.allPrice", is(30000)));
    }

    @Test
    @DisplayName("장바구니 상품 삭제 API 테스트: DELETE /api/items/{itemId}/cart")
    void removeCartItem_shouldRemoveItem() throws Exception {
        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(testItem, 5);
        cartRepository.save(cart);
        em.flush();
        em.clear();

        mockMvc.perform(delete("/api/cart/items/{itemId}", testItem.getId())
                .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems", hasSize(0)))
                .andExpect(jsonPath("$.allPrice", is(0)));

        Cart updatedCart = cartRepository.findByUser(testUser).orElseThrow();
        assertThat(updatedCart.getCartItems()).isEmpty();
    }

    @Test
    @DisplayName("장바구니 상품 삭제 응답은 남은 상품 이미지에 Pre-signed URL을 담는다")
    void removeCartItem_shouldReturnPresignedUrlForRemainingItems() throws Exception {
        Item secondItem = new Item("남은 상품", 5000, 10, "images/remaining-item.png");
        itemRepository.save(secondItem);

        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(testItem, 1);
        cart.addItem(secondItem, 1);
        cartRepository.save(cart);
        em.flush();
        em.clear();

        mockMvc.perform(delete("/api/cart/items/{itemId}", testItem.getId())
                        .with(user(testUserDetails)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems", hasSize(1)))
                .andExpect(jsonPath("$.cartItems[0].item.itemName", is(secondItem.getItemName())))
                .andExpect(jsonPath("$.cartItems[0].item.imageUrl", startsWith("https://")))
                .andExpect(jsonPath("$.allPrice", is(5000)));
    }
}
