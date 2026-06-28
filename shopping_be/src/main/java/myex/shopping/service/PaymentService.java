package myex.shopping.service;

import lombok.RequiredArgsConstructor;
import myex.shopping.domain.*;
import myex.shopping.dto.paymentdto.PaymentDto;
import myex.shopping.exception.AccessForbiddenException;
import myex.shopping.exception.PaymentException;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.payment.TossPaymentConfirmation;
import myex.shopping.payment.TossPaymentGateway;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.OrderRepository;
import myex.shopping.repository.PaymentRepository;
import myex.shopping.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PaymentService {

    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final CartService cartService;
    private final TossPaymentGateway tossPaymentGateway;

    public PaymentDto prepareMockPayment(User sessionUser) {
        return preparePayment(sessionUser, PaymentProvider.MOCK, "mock_");
    }

    public PaymentDto prepareTossPayment(User sessionUser) {
        return preparePayment(sessionUser, PaymentProvider.TOSS, "toss_");
    }

    private PaymentDto preparePayment(User sessionUser, PaymentProvider provider, String orderIdPrefix) {
        User user = loadUser(sessionUser);
        Cart cart = cartRepository.findByUser(user)
                .orElseThrow(() -> new PaymentException(
                        "EMPTY_CART",
                        "결제 준비 불가 : 장바구니에 상품을 담아주세요.",
                        HttpStatus.BAD_REQUEST));

        if (cart.getCartItems().isEmpty()) {
            throw new PaymentException(
                    "EMPTY_CART",
                    "결제 준비 불가 : 장바구니에 상품을 담아주세요.",
                    HttpStatus.BAD_REQUEST);
        }

        Order order = new Order(user);
        for (CartItem cartItem : cart.getCartItems().stream()
                .sorted(Comparator.comparing(entry -> entry.getItem().getId()))
                .toList()) {
            Item item = itemRepository.findByIdForUpdate(cartItem.getItem().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("item not found"));
            order.addOrderItem(new OrderItem(item, item.getPrice(), cartItem.getQuantity()));
        }

        order.reserveStockForPayment();
        orderRepository.save(order);

        Payment payment = new Payment(
                order,
                provider,
                orderIdPrefix + UUID.randomUUID(),
                order.getTotalPrice());
        paymentRepository.save(payment);
        cartService.deleteCart(cart.getId(), user.getId());

        return new PaymentDto(payment);
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public PaymentDto confirmMockPayment(User sessionUser, String paymentOrderId, int amount) {
        User user = loadUser(sessionUser);
        Payment payment = paymentRepository.findByPaymentOrderId(paymentOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("payment not found"));
        Order order = payment.getOrder();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessForbiddenException("결제를 승인할 권한이 없습니다.");
        }

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            return new PaymentDto(payment);
        }

        if (payment.getAmount() != amount) {
            payment.fail("요청 금액과 결제 금액이 일치하지 않습니다.");
            order.failPaymentAndReleaseStock();
            throw new PaymentException(
                    "PAYMENT_AMOUNT_MISMATCH",
                    "요청 금액과 결제 금액이 일치하지 않습니다.",
                    HttpStatus.BAD_REQUEST);
        }

        payment.approve("mock_" + payment.getPaymentOrderId());
        order.completePayment();
        return new PaymentDto(payment);
    }

    @Transactional(noRollbackFor = PaymentException.class)
    public PaymentDto confirmTossPayment(User sessionUser, String paymentKey, String orderId, int amount) {
        User user = loadUser(sessionUser);
        Payment payment = paymentRepository.findByPaymentOrderId(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("payment not found"));
        Order order = payment.getOrder();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessForbiddenException("결제를 승인할 권한이 없습니다.");
        }

        if (payment.getProvider() != PaymentProvider.TOSS) {
            throw new PaymentException(
                    "INVALID_PAYMENT_PROVIDER",
                    "토스 결제 주문이 아닙니다.",
                    HttpStatus.BAD_REQUEST);
        }

        if (payment.getStatus() == PaymentStatus.APPROVED) {
            return new PaymentDto(payment);
        }

        if (payment.getAmount() != amount) {
            payment.fail("요청 금액과 결제 금액이 일치하지 않습니다.");
            order.failPaymentAndReleaseStock();
            throw new PaymentException(
                    "PAYMENT_AMOUNT_MISMATCH",
                    "요청 금액과 결제 금액이 일치하지 않습니다.",
                    HttpStatus.BAD_REQUEST);
        }

        try {
            TossPaymentConfirmation confirmation =
                    tossPaymentGateway.confirm(paymentKey, payment.getPaymentOrderId(), payment.getAmount());

            if (confirmation.totalAmount() != payment.getAmount()) {
                payment.fail("토스 승인 금액과 결제 금액이 일치하지 않습니다.");
                order.failPaymentAndReleaseStock();
                throw new PaymentException(
                        "TOSS_AMOUNT_MISMATCH",
                        "토스 승인 금액과 결제 금액이 일치하지 않습니다.",
                        HttpStatus.BAD_GATEWAY);
            }

            payment.approve(confirmation.paymentKey());
            order.completePayment();
            return new PaymentDto(payment);
        } catch (PaymentException ex) {
            payment.fail(ex.getMessage());
            order.failPaymentAndReleaseStock();
            throw ex;
        }
    }

    public PaymentDto failMockPayment(User sessionUser, String paymentOrderId, String reason) {
        return failPayment(sessionUser, paymentOrderId, reason, PaymentProvider.MOCK);
    }

    public PaymentDto failTossPayment(User sessionUser, String paymentOrderId, String reason) {
        return failPayment(sessionUser, paymentOrderId, reason, PaymentProvider.TOSS);
    }

    private PaymentDto failPayment(
            User sessionUser,
            String paymentOrderId,
            String reason,
            PaymentProvider provider
    ) {
        User user = loadUser(sessionUser);
        Payment payment = paymentRepository.findByPaymentOrderId(paymentOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("payment not found"));
        Order order = payment.getOrder();

        if (!order.getUser().getId().equals(user.getId())) {
            throw new AccessForbiddenException("결제를 실패 처리할 권한이 없습니다.");
        }

        if (payment.getProvider() != provider) {
            throw new PaymentException(
                    "INVALID_PAYMENT_PROVIDER",
                    "결제 제공자가 일치하지 않습니다.",
                    HttpStatus.BAD_REQUEST);
        }

        String failureReason = reason == null || reason.isBlank()
                ? provider.name() + " 결제 실패 처리입니다."
                : reason.trim();

        payment.fail(failureReason);
        order.failPaymentAndReleaseStock();
        return new PaymentDto(payment);
    }

    private User loadUser(User sessionUser) {
        if (sessionUser == null || sessionUser.getId() == null) {
            throw new ResourceNotFoundException("user not found");
        }
        return userRepository.findById(sessionUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("user not found"));
    }
}
