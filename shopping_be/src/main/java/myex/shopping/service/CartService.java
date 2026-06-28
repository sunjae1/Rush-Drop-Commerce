package myex.shopping.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Cart;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.dto.cartdto.CartDto;
import myex.shopping.exception.InsufficientStockException;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class CartService {
    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;
    private final EntityManager em;
    private final PlatformTransactionManager transactionManager;

    public Cart findOrCreateCartForUser(User sessionUser) {
        Long userId = requireUserId(sessionUser);
        return retryOnCartUniqueViolation(() -> executeInWriteTransaction(() -> {
            User user = loadUser(userId);
            return cartRepository.findByUser(user)
                    .orElseGet(() -> createCart(user));
        }));
    }

    public Cart addItem(User sessionUser, Long itemId, int quantity) {
        Long userId = requireUserId(sessionUser);
        return retryOnCartUniqueViolation(() -> executeInWriteTransaction(() -> {
            User user = loadUser(userId);
            Item item = itemRepository.findById(itemId)
                    .orElseThrow(() -> new ResourceNotFoundException("item not found"));
            Cart cart = cartRepository.findByUser(user)
                    .orElseGet(() -> createCart(user));

            boolean added = cart.addItem(item, quantity);
            if (!added) {
                throw new InsufficientStockException("상품 재고 수량을 초과할 수 없습니다.");
            }

            em.flush();
            return cart;
        }));
    }

    // 장바구니 전체 DTO 변환
    @Transactional(readOnly = true)
    public CartDto findByUserByDto(User user) {
        return cartRepository.findByUser(user)
                .map(cart -> {
                    CartDto dto = new CartDto(cart);
                    imageService.resolveCartItemImageUrls(dto.getCartItems());
                    return dto;
                })
                .orElse(null); // null 해야 프론트에서 가능.
    }

    @Transactional(readOnly = false)
    public void save(Cart cart, User loginUser) {
        cartRepository.save(cart);
        log.info("cart 저장 후 cart 정보: {}", cart);
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 유저를 찾을 수 없습니다."));
        user.addCart(cart);
        log.info("cartService.save: user.Carts = {}", user.getCarts());
    }

    @Transactional(readOnly = false)
    public void update(Cart cart) {
        log.info("준영속 객체 Cart : {}", cart);

        Cart managedCart = em.merge(cart); // update문 예약.
        log.info("em.flush(); 전");
        em.flush(); // id는 hibernate가 merge()호출 중에 IdentifierGenerator 로 바로 ID 생성.
        // (다음 줄에 바로 Native Query가 나가지 않는 이상 지금 상태에선 필요 없음)
        log.info("em.flush() 후");

        // 세션과 영속 객체 같게 맞춤. --> cart: DB에서 꺼낸 준영속, managedCart : 영속 상태\
        log.info("DB에서 꺼낸 준영속 Cart.getId : {}", cart.getId());
        log.info("em.merge 후 영속 상태 manaedCart.getId : {}", managedCart.getId());
        // cart.setId(managedCart.getId());
        // cart.getCartItems().clear();

        /*
         * //DB-> 세션과 동기화.
         * for (CartItem managedCI : managedCart.getCartItems()) {
         * CartItem sessionCI = new CartItem();
         * sessionCI.setId(managedCI.getId());
         * sessionCI.setItem(managedCI.getItem());
         * sessionCI.setQuantity(managedCI.getQuantity());
         *
         * //양방향 연관관계
         * sessionCI.setCart(cart);
         * cart.getCartItems().add(sessionCI);
         * }
         */
    }

    @Transactional(readOnly = false)
    public Cart deleteItem(Long itemId, User loginUser) {
        // 없는 아이템 삭제 -> 예외 발생.
        Item item = itemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("cart not found"));
        User user = loadUser(requireUserId(loginUser));
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new ResourceNotFoundException("cart not found"));
        // 더티 체킹 - Commit 시점에 delete 쿼리 실행됨.
        cart.removeItem(item);
        return cart;
    }

    @Transactional(readOnly = false)
    public void deleteCart(Long cartId, Long userId) {
        log.info("cart 조회 전");
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("cart not found"));
        log.info("cart 조회 후");
        log.info("user 조회 전");
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));
        log.info("user 조회 후");
        // User CASCADETYPE.ALL 이라서 Cart에도 전파(더티 체킹)
        user.deleteCart(cart);
        log.info("deleteCart 후");
    }

    private Cart createCart(User user) {
        Cart newCart = new Cart();
        user.addCart(newCart);
        em.flush();
        return newCart;
    }

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));
    }

    private Long requireUserId(User sessionUser) {
        if (sessionUser == null) {
            throw new ResourceNotFoundException("user not found");
        }
        return sessionUser.getId();
    }

    private Cart executeInWriteTransaction(Supplier<Cart> action) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setReadOnly(false);
        return transactionTemplate.execute(status -> action.get());
    }

    private Cart retryOnCartUniqueViolation(Supplier<Cart> action) {
        try {
            return action.get();
        } catch (RuntimeException ex) {
            if (!isCartUniqueViolation(ex)) {
                throw ex;
            }

            log.info("cart unique 충돌 감지, 기존 장바구니 재조회 후 재시도");
            return action.get();
        }
    }

    private boolean isCartUniqueViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String message = current.getMessage();
            if (current instanceof DataIntegrityViolationException && message != null && isCartUniqueViolationMessage(message)) {
                return true;
            }
            if (message != null && isCartUniqueViolationMessage(message)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isCartUniqueViolationMessage(String message) {
        String normalizedMessage = message.toLowerCase();
        return normalizedMessage.contains("uk_cart_user")
                || normalizedMessage.contains("cart(user_id")
                || normalizedMessage.contains("cart (user_id")
                || normalizedMessage.contains("23505");
    }
}
