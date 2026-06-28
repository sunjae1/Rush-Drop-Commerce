package myex.shopping.repository.jpa;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import myex.shopping.domain.Payment;
import myex.shopping.repository.PaymentRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaPaymentRepository implements PaymentRepository {

    private final EntityManager em;

    @Override
    @Transactional(readOnly = false)
    public Payment save(Payment payment) {
        if (payment.getId() == null) {
            em.persist(payment);
            return payment;
        }
        return em.merge(payment);
    }

    @Override
    public Optional<Payment> findById(Long id) {
        return Optional.ofNullable(em.find(Payment.class, id));
    }

    @Override
    public Optional<Payment> findByPaymentOrderId(String paymentOrderId) {
        return em.createQuery("""
                        select distinct p from Payment p
                        join fetch p.order o
                        join fetch o.user u
                        left join fetch o.orderItems oi
                        left join fetch oi.item i
                        where p.paymentOrderId = :paymentOrderId
                        """, Payment.class)
                .setParameter("paymentOrderId", paymentOrderId)
                .getResultList()
                .stream()
                .findFirst();
    }
}
