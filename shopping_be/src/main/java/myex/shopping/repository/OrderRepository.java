package myex.shopping.repository;

import myex.shopping.domain.Order;
import myex.shopping.domain.User;

import java.util.List;
import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findByUser(User user);
    List<Order> findAll();
}
