package myex.shopping.repository.jpa;

import myex.shopping.domain.Item;
import myex.shopping.domain.Order;
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
@Import(JpaOrderRepository.class)
class JpaOrderRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private JpaOrderRepository orderRepository;

    private User user;
    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        user = new User("orderuser@example.com", "orderuser", "password");
        em.persist(user);

        item1 = new Item("itemA", 100, 10);
        em.persist(item1);

        item2 = new Item("itemB", 200, 20);
        em.persist(item2);

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("새로운 주문 저장 테스트")
    void save() {
        // given
        User managedUser = em.find(User.class, this.user.getId());
        Item managedItem = em.find(Item.class, this.item1.getId());
        
        Order order = new Order(managedUser);
        order.addOrderItem(new myex.shopping.domain.OrderItem(managedItem, managedItem.getPrice(), 2));

        // when
        Order savedOrder = orderRepository.save(order);
        em.flush();
        em.clear();

        // then
        assertThat(savedOrder.getId()).isNotNull();
        Order foundOrder = em.find(Order.class, savedOrder.getId());
        assertThat(foundOrder).isNotNull();
        assertThat(foundOrder.getUser().getId()).isEqualTo(managedUser.getId());
        assertThat(foundOrder.getOrderItems()).hasSize(1);
    }

    @Test
    @DisplayName("ID로 주문 조회")
    void findById() {
        // given
        User managedUser = em.find(User.class, this.user.getId());
        Order order = new Order(managedUser);
        order.addOrderItem(new myex.shopping.domain.OrderItem(item1, item1.getPrice(), 1));
        em.persistAndFlush(order);
        Long orderId = order.getId();
        em.clear();

        // when
        Optional<Order> foundOrderOpt = orderRepository.findById(orderId);

        // then
        assertThat(foundOrderOpt).isPresent();
        assertThat(foundOrderOpt.get().getId()).isEqualTo(orderId);
    }

    @Test
    @DisplayName("사용자로 주문 목록 조회")
    void findByUser() {
        // given
        User managedUser = em.find(User.class, this.user.getId());
        Item managedItem = em.find(Item.class, this.item1.getId());

        Order order1 = new Order(managedUser);
        order1.addOrderItem(new myex.shopping.domain.OrderItem(managedItem, managedItem.getPrice(), 1));
        em.persist(order1);

        Order order2 = new Order(managedUser);
        order2.addOrderItem(new myex.shopping.domain.OrderItem(managedItem, managedItem.getPrice(), 2));
        em.persist(order2);

        em.flush();
        em.clear();

        // when
        // We need to use a managed user instance for the query parameter
        User queryUser = em.find(User.class, managedUser.getId());
        List<Order> orders = orderRepository.findByUser(queryUser);

        // then
        assertThat(orders).hasSize(2);
    }

    @Test
    @DisplayName("모든 주문 조회")
    void findAll() {
        // given
        User managedUser = em.find(User.class, this.user.getId());
        Item managedItem1 = em.find(Item.class, this.item1.getId());
        Item managedItem2 = em.find(Item.class, this.item2.getId());

        User user2 = new User("user2@example.com", "user2", "password123");
        em.persist(user2);

        Order order1 = new Order(managedUser);
        order1.addOrderItem(new myex.shopping.domain.OrderItem(managedItem1, managedItem1.getPrice(), 1));
        em.persist(order1);

        Order order2 = new Order(user2);
        order2.addOrderItem(new myex.shopping.domain.OrderItem(managedItem2, managedItem2.getPrice(), 1));
        em.persist(order2);

        em.flush();
        em.clear();

        // when
        List<Order> allOrders = orderRepository.findAll();

        // then
        assertThat(allOrders).hasSize(2);
    }
}
