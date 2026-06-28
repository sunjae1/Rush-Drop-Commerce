package myex.shopping.service;

import jakarta.persistence.EntityManager;
import myex.shopping.domain.Cart;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.UserRepository;
import myex.shopping.support.RedisBackedSpringBootTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CartServiceIntegrationTest extends RedisBackedSpringBootTest {

    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("동시에 최초 장바구니 담기를 호출해도 사용자 장바구니는 하나만 생성된다")
    void addItemShouldRecoverFromConcurrentCartCreation() throws Exception {
        String unique = UUID.randomUUID().toString();
        User user = new User("cart-" + unique + "@example.com", "cart-user", "password123!");
        userService.save(user);

        Item item = new Item("concurrent-item-" + unique, 1000, 10);
        itemRepository.save(item);

        CountDownLatch startSignal = new CountDownLatch(1);
        Callable<Cart> addItemTask = () -> {
            startSignal.await();
            return cartService.addItem(user, item.getId(), 1);
        };

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        try {
            Future<Cart> first = executorService.submit(addItemTask);
            Future<Cart> second = executorService.submit(addItemTask);

            startSignal.countDown();

            first.get();
            second.get();
        } finally {
            executorService.shutdownNow();
        }

        entityManager.clear();

        Long cartCount = entityManager.createQuery(
                        "select count(c) from Cart c where c.user.id = :userId", Long.class)
                .setParameter("userId", user.getId())
                .getSingleResult();

        User reloadedUser = userRepository.findById(user.getId()).orElseThrow();
        Cart cart = cartRepository.findByUser(reloadedUser).orElseThrow();

        assertThat(cartCount).isEqualTo(1L);
        assertThat(cart.getCartItems()).hasSize(1);
        assertThat(cart.getCartItems().getFirst().getQuantity()).isEqualTo(2);
    }
}
