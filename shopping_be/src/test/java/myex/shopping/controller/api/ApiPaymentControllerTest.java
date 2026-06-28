package myex.shopping.controller.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import myex.shopping.domain.*;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.OrderRepository;
import myex.shopping.repository.PaymentRepository;
import myex.shopping.repository.UserRepository;
import myex.shopping.payment.TossPaymentConfirmation;
import myex.shopping.payment.TossPaymentGateway;
import myex.shopping.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ApiPaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ItemRepository itemRepository;
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private PaymentRepository paymentRepository;
    @Autowired
    private CartService cartService;
    @MockitoBean
    private TossPaymentGateway tossPaymentGateway;

    private User testUser;
    private Item testItem;
    private PrincipalDetails testUserDetails;

    @BeforeEach
    void setUp() {
        testUser = new User("pay-user@example.com", "결제유저", "password");
        userRepository.save(testUser);

        testItem = new Item("Mock 결제 상품", 15000, 10, "path");
        itemRepository.save(testItem);

        testUserDetails = new PrincipalDetails(testUser);
    }

    @Test
    @DisplayName("Mock 결제 준비 후 승인하면 주문은 PAID, 결제는 APPROVED가 된다")
    void prepareAndConfirmMockPayment() throws Exception {
        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(testItem, 2);

        String prepareBody = mockMvc.perform(post("/api/payments/mock/prepare")
                        .with(user(testUserDetails)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider", is("MOCK")))
                .andExpect(jsonPath("$.status", is("READY")))
                .andExpect(jsonPath("$.amount", is(30000)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode prepared = objectMapper.readTree(prepareBody);
        String paymentOrderId = prepared.get("paymentOrderId").asText();
        Long orderId = prepared.get("orderId").asLong();

        assertThat(testItem.getQuantity()).isEqualTo(8);
        assertThat(cartRepository.findByUser(testUser)).isEmpty();
        assertThat(orderRepository.findById(orderId).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.PAYMENT_PENDING);

        String confirmBody = """
                {
                  "paymentOrderId": "%s",
                  "amount": 30000
                }
                """.formatted(paymentOrderId);

        mockMvc.perform(post("/api/payments/mock/confirm")
                        .with(user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.providerPaymentKey", is("mock_" + paymentOrderId)));

        Payment payment = paymentRepository.findByPaymentOrderId(paymentOrderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(testItem.getQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("Mock 결제 승인 금액이 다르면 결제 실패 처리하고 예약 재고를 복구한다")
    void confirmMockPayment_amountMismatch() throws Exception {
        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(testItem, 2);

        String prepareBody = mockMvc.perform(post("/api/payments/mock/prepare")
                        .with(user(testUserDetails)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentOrderId = objectMapper.readTree(prepareBody).get("paymentOrderId").asText();
        String confirmBody = """
                {
                  "paymentOrderId": "%s",
                  "amount": 1
                }
                """.formatted(paymentOrderId);

        mockMvc.perform(post("/api/payments/mock/confirm")
                        .with(user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PAYMENT_AMOUNT_MISMATCH")));

        Payment payment = paymentRepository.findByPaymentOrderId(paymentOrderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(testItem.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Mock 결제 실패 처리 시 주문은 PAYMENT_FAILED가 되고 예약 재고를 복구한다")
    void failMockPayment() throws Exception {
        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(testItem, 3);

        String prepareBody = mockMvc.perform(post("/api/payments/mock/prepare")
                        .with(user(testUserDetails)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("READY")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentOrderId = objectMapper.readTree(prepareBody).get("paymentOrderId").asText();
        assertThat(testItem.getQuantity()).isEqualTo(7);

        String failBody = """
                {
                  "paymentOrderId": "%s",
                  "reason": "사용자 카드 승인 실패 테스트"
                }
                """.formatted(paymentOrderId);

        mockMvc.perform(post("/api/payments/mock/fail")
                        .with(user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(failBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("FAILED")))
                .andExpect(jsonPath("$.failureReason", is("사용자 카드 승인 실패 테스트")));

        Payment payment = paymentRepository.findByPaymentOrderId(paymentOrderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(testItem.getQuantity()).isEqualTo(10);
    }

    @Test
    @DisplayName("Toss 결제 준비 후 승인하면 토스 결제키를 저장하고 주문은 PAID가 된다")
    void prepareAndConfirmTossPayment() throws Exception {
        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(testItem, 2);

        String prepareBody = mockMvc.perform(post("/api/payments/toss/prepare")
                        .with(user(testUserDetails)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.provider", is("TOSS")))
                .andExpect(jsonPath("$.status", is("READY")))
                .andExpect(jsonPath("$.amount", is(30000)))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode prepared = objectMapper.readTree(prepareBody);
        String orderId = prepared.get("paymentOrderId").asText();
        Long savedOrderId = prepared.get("orderId").asLong();

        when(tossPaymentGateway.confirm(eq("toss_payment_key"), eq(orderId), eq(30000)))
                .thenReturn(new TossPaymentConfirmation("toss_payment_key", orderId, "DONE", 30000));

        String confirmBody = """
                {
                  "paymentKey": "toss_payment_key",
                  "orderId": "%s",
                  "amount": 30000
                }
                """.formatted(orderId);

        mockMvc.perform(post("/api/payments/toss/confirm")
                        .with(user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.provider", is("TOSS")))
                .andExpect(jsonPath("$.providerPaymentKey", is("toss_payment_key")));

        Payment payment = paymentRepository.findByPaymentOrderId(orderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(payment.getOrder().getId()).isEqualTo(savedOrderId);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(testItem.getQuantity()).isEqualTo(8);
    }

    @Test
    @DisplayName("Toss 승인 금액이 다르면 토스 승인 API를 호출하지 않고 예약 재고를 복구한다")
    void confirmTossPayment_amountMismatch() throws Exception {
        Cart cart = cartService.findOrCreateCartForUser(testUser);
        cart.addItem(testItem, 2);

        String prepareBody = mockMvc.perform(post("/api/payments/toss/prepare")
                        .with(user(testUserDetails)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String orderId = objectMapper.readTree(prepareBody).get("paymentOrderId").asText();
        String confirmBody = """
                {
                  "paymentKey": "toss_payment_key",
                  "orderId": "%s",
                  "amount": 1
                }
                """.formatted(orderId);

        mockMvc.perform(post("/api/payments/toss/confirm")
                        .with(user(testUserDetails))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(confirmBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("PAYMENT_AMOUNT_MISMATCH")));

        verify(tossPaymentGateway, never()).confirm(eq("toss_payment_key"), eq(orderId), eq(1));

        Payment payment = paymentRepository.findByPaymentOrderId(orderId).orElseThrow();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.PAYMENT_FAILED);
        assertThat(testItem.getQuantity()).isEqualTo(10);
    }
}
