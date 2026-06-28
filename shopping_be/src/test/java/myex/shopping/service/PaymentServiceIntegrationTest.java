package myex.shopping.service;

import myex.shopping.domain.Cart;
import myex.shopping.domain.Item;
import myex.shopping.domain.OrderStatus;
import myex.shopping.domain.Payment;
import myex.shopping.domain.PaymentProvider;
import myex.shopping.domain.PaymentStatus;
import myex.shopping.domain.User;
import myex.shopping.dto.paymentdto.PaymentDto;
import myex.shopping.exception.PaymentException;
import myex.shopping.payment.TossPaymentConfirmation;
import myex.shopping.payment.TossPaymentGateway;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.PaymentRepository;
import myex.shopping.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
class PaymentServiceIntegrationTest {

    @Autowired
    private PaymentService paymentService;
    @Autowired
    private CartService cartService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @MockitoBean
    private TossPaymentGateway tossPaymentGateway;

    private User user;
    private Item item;

    @BeforeEach
    void setUp() {
        user = userRepository.save(new User("service-pay-user@example.com", "서비스결제유저", "password"));
        item = itemRepository.save(new Item("서비스 Toss 상품", 21000, 5, "path"));
    }

    @Test
    @DisplayName("Toss 결제 준비는 장바구니를 결제 대기 주문으로 전환하고 재고를 예약한다")
    void prepareTossPayment_reservesStockAndCreatesReadyPayment() {
        Cart cart = cartService.findOrCreateCartForUser(user);
        cart.addItem(item, 2);

        PaymentDto prepared = paymentService.prepareTossPayment(user);

        Payment payment = paymentRepository.findByPaymentOrderId(prepared.getPaymentOrderId()).orElseThrow();
        assertThat(prepared.getProvider()).isEqualTo("TOSS");
        assertThat(prepared.getStatus()).isEqualTo("READY");
        assertThat(prepared.getAmount()).isEqualTo(42000);
        assertThat(payment.getProvider()).isEqualTo(PaymentProvider.TOSS);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.READY);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAYMENT_PENDING);
        assertThat(item.getQuantity()).isEqualTo(3);
        assertThat(cartRepository.findByUser(user)).isEmpty();
    }

    @Test
    @DisplayName("Toss 승인 성공은 저장 금액으로 게이트웨이를 호출하고 주문/결제를 승인 처리한다")
    void confirmTossPayment_approvesPaymentAndOrder() {
        Cart cart = cartService.findOrCreateCartForUser(user);
        cart.addItem(item, 2);
        PaymentDto prepared = paymentService.prepareTossPayment(user);

        when(tossPaymentGateway.confirm(eq("payment_key"), eq(prepared.getPaymentOrderId()), eq(42000)))
                .thenReturn(new TossPaymentConfirmation("payment_key", prepared.getPaymentOrderId(), "DONE", 42000));

        PaymentDto confirmed = paymentService.confirmTossPayment(
                user,
                "payment_key",
                prepared.getPaymentOrderId(),
                42000);

        Payment payment = paymentRepository.findByPaymentOrderId(prepared.getPaymentOrderId()).orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo("APPROVED");
        assertThat(confirmed.getProviderPaymentKey()).isEqualTo("payment_key");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(item.getQuantity()).isEqualTo(3);
        verify(tossPaymentGateway).confirm("payment_key", prepared.getPaymentOrderId(), 42000);
    }

    @Test
    @DisplayName("Toss 요청 금액이 다르면 게이트웨이를 호출하지 않고 결제 실패와 재고 복구를 수행한다")
    void confirmTossPayment_requestAmountMismatchFailsBeforeGateway() {
        Cart cart = cartService.findOrCreateCartForUser(user);
        cart.addItem(item, 2);
        PaymentDto prepared = paymentService.prepareTossPayment(user);

        assertThatThrownBy(() -> paymentService.confirmTossPayment(
                user,
                "payment_key",
                prepared.getPaymentOrderId(),
                1))
                .isInstanceOf(PaymentException.class)
                .hasMessage("요청 금액과 결제 금액이 일치하지 않습니다.");

        Payment payment = paymentRepository.findByPaymentOrderId(prepared.getPaymentOrderId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(item.getQuantity()).isEqualTo(5);
        verify(tossPaymentGateway, never()).confirm(eq("payment_key"), eq(prepared.getPaymentOrderId()), eq(1));
    }

    @Test
    @DisplayName("Toss 승인 응답 금액이 다르면 실패 처리하고 예약 재고를 복구한다")
    void confirmTossPayment_gatewayAmountMismatchFailsAndRestoresStock() {
        Cart cart = cartService.findOrCreateCartForUser(user);
        cart.addItem(item, 2);
        PaymentDto prepared = paymentService.prepareTossPayment(user);

        when(tossPaymentGateway.confirm(eq("payment_key"), eq(prepared.getPaymentOrderId()), eq(42000)))
                .thenReturn(new TossPaymentConfirmation("payment_key", prepared.getPaymentOrderId(), "DONE", 1));

        assertThatThrownBy(() -> paymentService.confirmTossPayment(
                user,
                "payment_key",
                prepared.getPaymentOrderId(),
                42000))
                .isInstanceOf(PaymentException.class)
                .hasMessage("토스 승인 금액과 결제 금액이 일치하지 않습니다.");

        Payment payment = paymentRepository.findByPaymentOrderId(prepared.getPaymentOrderId()).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(item.getQuantity()).isEqualTo(5);
    }

    @Test
    @DisplayName("Toss 결제창 실패 콜백은 결제 실패와 예약 재고 복구를 수행한다")
    void failTossPayment_marksFailedAndRestoresStock() {
        Cart cart = cartService.findOrCreateCartForUser(user);
        cart.addItem(item, 2);
        PaymentDto prepared = paymentService.prepareTossPayment(user);

        PaymentDto failed = paymentService.failTossPayment(
                user,
                prepared.getPaymentOrderId(),
                "사용자가 결제창을 닫았습니다.");

        Payment payment = paymentRepository.findByPaymentOrderId(prepared.getPaymentOrderId()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getFailureReason()).isEqualTo("사용자가 결제창을 닫았습니다.");
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(item.getQuantity()).isEqualTo(5);
    }
}
