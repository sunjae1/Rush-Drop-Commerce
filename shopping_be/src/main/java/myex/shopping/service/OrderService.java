package myex.shopping.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.*;
import myex.shopping.dto.mypagedto.MyPageOrderDto;
import myex.shopping.dto.orderdto.OrderDBDto;
import myex.shopping.dto.orderdto.OrderDto;
import myex.shopping.exception.AccessForbiddenException;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final ItemRepository itemRepository;
    private final OrderRepository orderRepository;
    private final CartRepository cartRepository;

    // 장바구니 --> 주문 으로 전환.
    public Order checkout(User user) {
        Order order = new Order(user);
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

        for (CartItem cartItem : cart.getCartItems().stream()
                .sorted(Comparator.comparing(cartItem -> cartItem.getItem().getId()))
                .toList()) {
            // DB에서 직접 item 조회해서 영속 상태 item 가져옴.(재고 반영을 위해서)
            Item persistentItem = itemRepository.findByIdForUpdate(cartItem.getItem().getId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다."));

            order.addOrderItem(new OrderItem(persistentItem, cartItem.getItem().getPrice(), cartItem.getQuantity()));
        }

        // 재고 감소 : 더티체킹 UPDATE 쿼리.
        order.confirmOrder();
        log.info("order.confirmOrder() 재고 감소 후");
        // order-orderItem CASCADE.ALL 이여서 save시 같이 INSERT 문 날라가고, 같이 영속성 컨텍스트로 관리됨.
        orderRepository.save(order); // GenerationType.IDENTITY 이므로, save() 호출 즉시 INSERT 쿼리 나감.
        log.info("checkout 메소드 save(order) 후");
        return order;
    }

    public Order orderCancel(Long id, User loginUser) {
        // 더티 체킹.
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("order not found"));
        // 재고 올리고 상태 CANCELLED 로 바뀜, 주문내역은 사라지지 않음.(기록용)

        // 주문자와 로그인한 사용자가 같은지 확인
        if (order.getUser().getId().equals(loginUser.getId())) {
            order.getOrderItems().stream()
                    .sorted(Comparator.comparing(orderItem -> orderItem.getItem().getId()))
                    .forEach(orderItem -> itemRepository.findByIdForUpdate(orderItem.getItem().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("item not found")));
            order.cancel();
            return order;
        } else {
            throw new AccessForbiddenException("주문을 취소할 권한이 없습니다.");
        }

    }

    public List<MyPageOrderDto> changeToOrderDtoList(User user) {
        return orderRepository.findByUser(user)
                .stream()
                .map(MyPageOrderDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDto> findOrderDtosByUser(User user) {
        return orderRepository.findByUser(user)
                .stream()
                .map(OrderDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<OrderDBDto> findAllOrderDtos() {
        return orderRepository.findAll()
                .stream()
                .map(OrderDBDto::new)
                .collect(Collectors.toList());
    }

}
