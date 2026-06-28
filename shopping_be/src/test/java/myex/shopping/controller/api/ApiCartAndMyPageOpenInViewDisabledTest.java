package myex.shopping.controller.api;

import jakarta.persistence.EntityManager;
import myex.shopping.domain.Cart;
import myex.shopping.domain.Category;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.UserRepository;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import myex.shopping.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = "spring.jpa.open-in-view=false")
class ApiCartAndMyPageOpenInViewDisabledTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private JpaCategoryRepository categoryRepository;

    @Autowired
    private EntityManager em;

    private User testUser;
    private PrincipalDetails testUserDetails;
    private Item categorizedItem;

    @BeforeEach
    void setUp() {
        testUser = new User("prod-like-cart@example.com", "운영유사유저", "password");
        userRepository.save(testUser);

        Category category = new Category();
        category.setName("상의");
        categoryRepository.save(category);

        categorizedItem = new Item("카테고리 상품", 10000, 10, "images/category-item.png");
        categorizedItem.changeCategory(category);
        itemRepository.save(categorizedItem);

        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(categorizedItem, 1);
        cartRepository.save(cart);

        testUserDetails = new PrincipalDetails(testUser);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("open-in-view=false 에서도 장바구니 조회는 카테고리 정보를 반환한다")
    void getCart_shouldReturnCategoryInfo_whenOpenInViewIsDisabled() throws Exception {
        mockMvc.perform(get("/api/cart")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems[0].item.itemName", is(categorizedItem.getItemName())))
                .andExpect(jsonPath("$.cartItems[0].item.categoryName", is("상의")));
    }

    @Test
    @DisplayName("open-in-view=false 에서도 마이페이지 조회는 장바구니 상품 카테고리를 반환한다")
    void myPage_shouldReturnCategoryInfo_whenOpenInViewIsDisabled() throws Exception {
        mockMvc.perform(get("/api/myPage")
                        .with(SecurityMockMvcRequestPostProcessors.user(testUserDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartItems[0].itemName", is(categorizedItem.getItemName())))
                .andExpect(jsonPath("$.cartItems[0].categoryName", is("상의")));
    }
}
