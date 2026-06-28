package myex.shopping.service;

import myex.shopping.domain.*;
import myex.shopping.dto.mypagedto.MyPageOrderDto;
import myex.shopping.dto.orderdto.OrderDBDto;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import org.mockito.InOrder;


@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CartRepository cartRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("체크아웃 시 주문이 생성되고 재고가 감소한다")
    void checkout() {
        // given
        User user = new User();
        user.setId(1L);

        Item item1 = new Item("itemA", 10000, 10);
        item1.setId(1L);
        Item item2 = new Item("itemB", 20000, 20);
        item2.setId(2L);

        Cart cart = new Cart();
        cart.setUser(user);
        cart.addItem(item2, 3);
        cart.addItem(item1, 2);

        Order order = new Order(user);

        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(item2));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(cartRepository.findByUser(any(User.class))).thenReturn(Optional.of(cart));

        // when
        Order resultOrder = orderService.checkout(user);

        // then
        assertThat(resultOrder.getOrderItems()).hasSize(2);
        assertThat(resultOrder.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(item1.getQuantity()).isEqualTo(8); // 10 - 2
        assertThat(item2.getQuantity()).isEqualTo(17); // 20 - 3

        InOrder inOrder = inOrder(itemRepository);
        inOrder.verify(itemRepository).findByIdForUpdate(1L);
        inOrder.verify(itemRepository).findByIdForUpdate(2L);
    }

    @Test
    @DisplayName("주문을 취소하면 재고가 다시 증가한다")
    void orderCancel() {
        // given
        User user = new User();
        user.setId(1L);

        Item item1 = new Item("itemA", 10000, 8);
        item1.setId(1L);

        Order order = new Order(user);
        order.addOrderItem(new OrderItem(item1, 10000, 2));
        order.setStatus(OrderStatus.PAID);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(itemRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(item1));

        // when
        orderService.orderCancel(1L, user);

        // then
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(item1.getQuantity()).isEqualTo(10); // 8 + 2
    }

    @Test
    @DisplayName("특정 사용자의 주문 목록을 DTO로 변환하여 반환한다")
    void changeToOrderDtoList() {
        // given
        User user = new User();
        user.setId(1L);

        Order order1 = new Order(user);
        Order order2 = new Order(user);
        when(orderRepository.findByUser(user)).thenReturn(Arrays.asList(order1, order2));

        // when
        List<MyPageOrderDto> result = orderService.changeToOrderDtoList(user);

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("모든 주문 목록을 DTO로 변환하여 반환한다")
    void findAllOrderDtos() {
        // given
        User user1 = new User();
        user1.setId(1L);
        User user2 = new User();
        user2.setId(2L);
        
        Order order1 = new Order(user1);
        Order order2 = new Order(user2);
        when(orderRepository.findAll()).thenReturn(Arrays.asList(order1, order2));

        // when
        List<OrderDBDto> result = orderService.findAllOrderDtos();

        // then
        assertThat(result).hasSize(2);
    }
}
