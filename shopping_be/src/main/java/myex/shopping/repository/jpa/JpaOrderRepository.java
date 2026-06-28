package myex.shopping.repository.jpa;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import myex.shopping.domain.Order;
import myex.shopping.domain.User;
import myex.shopping.repository.OrderRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Primary
@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaOrderRepository implements OrderRepository {

    private final EntityManager em;

    @Override
    @Transactional(readOnly = false)
    public Order save(Order order) {
        em.persist(order);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        return Optional.ofNullable(em.find(Order.class, id));
    }

    //JPQL은 DB 테이블이 아니라 엔티티 기준으로 작성.
    // ✅ 핵심: JPQL은 엔티티 필드 기준이지, FK 컬럼이 실제 DB에 매핑된 컬럼명과는 무관
    //Native Query는 DB 컬럼명 직접 참조가 필요할 때만 사용.
    //SELECT * FROM orders WHERE user_id = 3 이런거.
    @Override
    public List<Order> findByUser(User user) {
        return em.createQuery("select distinct o from Order o " +
                        "join fetch o.orderItems oi " +
                        "join fetch oi.item i " +
                        "where o.user = :user", Order.class)
                .setParameter("user", user)
                .getResultList();
    }


    //fetch join으로 수정.
    @Override
    public List<Order> findAll() {

        return em.createQuery("select distinct o from Order o " +
                        "join fetch o.user u " +
                        "join fetch o.orderItems oi " +
                        "join fetch oi.item i", Order.class)
                .getResultList();
    }
}
