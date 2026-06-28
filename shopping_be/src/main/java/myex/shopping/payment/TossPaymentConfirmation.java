package myex.shopping.payment;

public record TossPaymentConfirmation(
        String paymentKey,
        String orderId,
        String status,
        int totalAmount
) {
}
