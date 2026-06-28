package myex.shopping.repository.jpa;

import myex.shopping.domain.Cart;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaCartRepository.class)
class JpaCartRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JpaCartRepository cartRepository;

    private User user;
    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        user = new User("cartuser@example.com", "cartuser", "password");
        em.persist(user);

        item1 = new Item("itemA", 100, 10);
        em.persist(item1);
        
        item2 = new Item("itemB", 200, 20);
        em.persist(item2);
        
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("새로운 장바구니 저장 테스트")
    void save() {
        // given
        Cart cart = new Cart();
        User foundUser = em.find(User.class, user.getId());
        cart.setUser(foundUser);
        cart.addItem(item1, 2);

        // when
        Cart savedCart = cartRepository.save(cart);
        em.flush();
        em.clear();

        // then
        assertThat(savedCart.getId()).isNotNull();
        Cart foundCart = em.find(Cart.class, savedCart.getId());
        assertThat(foundCart).isNotNull();
        assertThat(foundCart.getUser().getId()).isEqualTo(foundUser.getId());
        assertThat(foundCart.getCartItems()).hasSize(1);
    }

    @Test
    @DisplayName("ID로 장바구니 조회 (연관된 아이템과 함께)")
    void findById() {
        // given
        User foundUser = em.find(User.class, user.getId());
        Cart cart = new Cart();
        cart.setUser(foundUser);
        cart.addItem(item1, 1);
        cart.addItem(item2, 2);
        em.persistAndFlush(cart);
        em.clear();
        
        // when
        Optional<Cart> foundCartOpt = cartRepository.findById(cart.getId());

        // then
        assertThat(foundCartOpt).isPresent();
        Cart foundCart = foundCartOpt.get();
        assertThat(foundCart.getCartItems()).hasSize(2);
        assertThat(foundCart.getCartItems().get(0).getItem().getItemName()).isEqualTo("itemA");
    }

    @Test
    @DisplayName("사용자로 장바구니 조회")
    void findByUser() {
        // given
        User foundUser = em.find(User.class, user.getId());
        Cart cart = new Cart();
        cart.setUser(foundUser);
        em.persistAndFlush(cart);
        em.clear();

        // when
        Optional<Cart> foundCartOpt = cartRepository.findByUser(foundUser);

        // then
        assertThat(foundCartOpt).isPresent();
        assertThat(foundCartOpt.get().getUser().getId()).isEqualTo(foundUser.getId());
    }

    @Test
    @DisplayName("모든 장바구니 조회")
    void findAll() {
        // given
        User managedUser = em.find(User.class, this.user.getId());
        Item managedItem1 = em.find(Item.class, this.item1.getId());
        Item managedItem2 = em.find(Item.class, this.item2.getId());
        
        User user2 = new User("user2@example.com", "user2", "password2");
        em.persist(user2);
        
        Cart cart1 = new Cart();
        cart1.setUser(managedUser);
        cart1.addItem(managedItem1, 1);
        em.persist(cart1);

        Cart cart2 = new Cart();
        cart2.setUser(user2);
        cart2.addItem(managedItem2, 1);
        em.persist(cart2);
        
        em.flush();
        em.clear();

        // when
        List<Cart> allCarts = cartRepository.findAll();

        // then
        // The JPQL is an inner join, so it will only return carts with items
        assertThat(allCarts).hasSize(2);
    }

    @Test
    @DisplayName("장바구니 삭제 테스트")
    void delete() {
        // given
        Cart cart = new Cart();
        cart.setUser(user);
        em.persistAndFlush(cart);
        Long cartId = cart.getId();
        em.clear();
        
        // when
        cartRepository.delete(cartId);
        em.flush();
        em.clear();

        // then
        Cart deletedCart = em.find(Cart.class, cartId);
        assertThat(deletedCart).isNull();
    }
}
