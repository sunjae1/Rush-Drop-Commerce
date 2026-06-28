package myex.shopping.repository;

import myex.shopping.domain.Cart;
import myex.shopping.domain.CartItem;
import myex.shopping.domain.User;

import java.util.List;
import java.util.Optional;

public interface CartRepository {
    Cart save (Cart cart);
    Optional<Cart> findById(Long id);

    Optional<Cart> findByUser(User user);

    List<Cart> findAll();
    void delete(Long id);
}
