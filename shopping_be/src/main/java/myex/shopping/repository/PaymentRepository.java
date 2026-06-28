package myex.shopping.repository;

import myex.shopping.domain.Payment;

import java.util.Optional;

public interface PaymentRepository {
    Payment save(Payment payment);
    Optional<Payment> findById(Long id);
    Optional<Payment> findByPaymentOrderId(String paymentOrderId);
}
