package myex.shopping.dto.paymentdto;

import lombok.Getter;
import myex.shopping.domain.Payment;

import java.time.LocalDateTime;

@Getter
public class PaymentDto {
    private Long id;
    private Long orderId;
    private String paymentOrderId;
    private String provider;
    private String status;
    private int amount;
    private String providerPaymentKey;
    private LocalDateTime requestedAt;
    private LocalDateTime approvedAt;
    private String failureReason;

    public PaymentDto(Payment payment) {
        this.id = payment.getId();
        this.orderId = payment.getOrder().getId();
        this.paymentOrderId = payment.getPaymentOrderId();
        this.provider = payment.getProvider().name();
        this.status = payment.getStatus().name();
        this.amount = payment.getAmount();
        this.providerPaymentKey = payment.getProviderPaymentKey();
        this.requestedAt = payment.getRequestedAt();
        this.approvedAt = payment.getApprovedAt();
        this.failureReason = payment.getFailureReason();
    }
}
