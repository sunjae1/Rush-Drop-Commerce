package myex.shopping.payment;

public interface TossPaymentGateway {
    TossPaymentConfirmation confirm(String paymentKey, String orderId, int amount);
}
