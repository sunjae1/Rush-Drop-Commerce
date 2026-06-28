package myex.shopping.repository.memory;

import myex.shopping.domain.Order;
import myex.shopping.domain.User;
import myex.shopping.repository.OrderRepository;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Repository
public class MemoryOrderRepository implements OrderRepository {
    private final Map<Long, Order> store = new HashMap<>();
    private final AtomicLong sequence = new AtomicLong(0);

    //주문 저장
    @Override
    public Order save(Order order) {
        long id = sequence.incrementAndGet();
        order.setId(id);
        store.put(id, order);
        return order;
    }

    //id로 찾기
    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    //User 로 Order 전체 찾기. (사용자가 주문한 내역 전부 호출)
    @Override
    public List<Order> findByUser(User user) {
        return store.values().stream()
                .filter(order -> order.getUser().equals(user))
                .collect(Collectors.toList());
    }


    //전체 주문 반환.
    @Override
    public List<Order> findAll() {
        return new ArrayList<>(store.values());
    }

}
