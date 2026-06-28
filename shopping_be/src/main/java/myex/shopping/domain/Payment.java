package myex.shopping.domain;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_order", columnNames = "order_id"),
                @UniqueConstraint(name = "uk_payment_order_id", columnNames = "payment_order_id")
        })
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "payment_order_id", nullable = false, length = 80)
    private String paymentOrderId;

    @Column(name = "provider_payment_key", length = 120)
    private String providerPaymentKey;

    @Column(nullable = false)
    private int amount;

    @Column(nullable = false)
    private LocalDateTime requestedAt;

    private LocalDateTime approvedAt;

    @Column(length = 255)
    private String failureReason;

    protected Payment() {
    }

    public Payment(Order order, PaymentProvider provider, String paymentOrderId, int amount) {
        this.order = order;
        this.provider = provider;
        this.paymentOrderId = paymentOrderId;
        this.amount = amount;
        this.status = PaymentStatus.READY;
        this.requestedAt = LocalDateTime.now();
    }

    public void approve(String providerPaymentKey) {
        if (this.status == PaymentStatus.APPROVED) {
            return;
        }
        if (this.status != PaymentStatus.READY) {
            throw new IllegalStateException("승인할 수 없는 결제 상태입니다.");
        }

        this.providerPaymentKey = providerPaymentKey;
        this.status = PaymentStatus.APPROVED;
        this.approvedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    public void fail(String failureReason) {
        if (this.status == PaymentStatus.APPROVED) {
            throw new IllegalStateException("이미 승인된 결제는 실패 처리할 수 없습니다.");
        }
        if (this.status == PaymentStatus.FAILED) {
            return;
        }

        this.status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
    }
}
