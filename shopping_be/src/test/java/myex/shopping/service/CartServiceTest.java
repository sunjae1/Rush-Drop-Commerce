package myex.shopping.service;

import jakarta.persistence.EntityManager;
import myex.shopping.domain.Cart;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private EntityManager em;

    @Mock
    private PlatformTransactionManager transactionManager;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        cartService = new CartService(
                itemRepository,
                cartRepository,
                userRepository,
                imageService,
                em,
                transactionManager
        );
    }

    @Test
    @DisplayName("사용자의 기존 장바구니를 찾는다")
    void findOrCreateCartForUser_ExistingCart() {
        // given
        User user = new User();
        user.setId(1L);
        Cart existingCart = new Cart();
        existingCart.setUser(user);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(existingCart));

        // when
        Cart cart = cartService.findOrCreateCartForUser(user);

        // then
        assertThat(cart).isEqualTo(existingCart);
    }

    @Test
    @DisplayName("사용자에게 장바구니가 없으면 새로 생성한다")
    void findOrCreateCartForUser_NewCart() {
        // given
        User user = new User();
        user.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.empty());

        // when
        Cart cart = cartService.findOrCreateCartForUser(user);

        // then
        assertThat(cart).isNotNull();
        assertThat(cart.getUser()).isEqualTo(user);
        assertThat(user.getCarts()).contains(cart);
    }

    @Test
    @DisplayName("장바구니에서 아이템을 삭제한다")
    void deleteItem() {
        // given
        User user = new User();
        user.setId(1L);
        Item item = new Item("itemA", 100, 10);
        item.setId(1L);
        Cart cart = new Cart();
        cart.addItem(item, 1);
        user.addCart(cart);

        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cartRepository.findByUser(user)).thenReturn(Optional.of(cart));

        // when
        cartService.deleteItem(1L, user);

        // then
        assertThat(cart.getCartItems()).isEmpty();
    }

    @Test
    @DisplayName("장바구니를 저장하고 사용자와 연결한다")
    void save() {
        // given
        User user = new User();
        user.setId(1L);
        Cart cart = new Cart();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // when
        cartService.save(cart, user);

        // then
        verify(cartRepository).save(cart);
        assertThat(user.getCarts()).contains(cart);
    }

    @Test
    @DisplayName("장바구니를 삭제한다")
    void deleteCart() {
        // given
        Long cartId = 1L;
        Long userId = 1L;
        User user = spy(new User());
        Cart cart = new Cart();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(cartRepository.findById(cartId)).thenReturn(Optional.of(cart));

        // when
        cartService.deleteCart(cartId, userId);

        // then
        verify(user).deleteCart(cart);
    }
}
