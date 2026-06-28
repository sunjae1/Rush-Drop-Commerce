package myex.shopping.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import myex.shopping.dto.paymentdto.PaymentConfirmRequest;
import myex.shopping.dto.paymentdto.PaymentDto;
import myex.shopping.dto.paymentdto.PaymentFailRequest;
import myex.shopping.dto.paymentdto.TossPaymentConfirmRequest;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/payments")
@Tag(name = "Payment", description = "결제 관련 API")
public class ApiPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/mock/prepare")
    @Operation(summary = "Mock 결제 준비", description = "장바구니를 결제 대기 주문으로 전환하고 재고를 예약합니다.")
    public ResponseEntity<?> prepareMockPayment(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        PaymentDto payment = paymentService.prepareMockPayment(principalDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PostMapping("/mock/confirm")
    @Operation(summary = "Mock 결제 승인", description = "Mock 결제 금액을 검증하고 주문을 결제 완료 상태로 변경합니다.")
    public ResponseEntity<?> confirmMockPayment(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                @Valid @RequestBody PaymentConfirmRequest request) {
        if (principalDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        PaymentDto payment = paymentService.confirmMockPayment(
                principalDetails.getUser(),
                request.getPaymentOrderId(),
                request.getAmount());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/mock/fail")
    @Operation(summary = "Mock 결제 실패", description = "Mock 결제를 실패 처리하고 예약 재고를 복구합니다.")
    public ResponseEntity<?> failMockPayment(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                             @Valid @RequestBody PaymentFailRequest request) {
        if (principalDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        PaymentDto payment = paymentService.failMockPayment(
                principalDetails.getUser(),
                request.getPaymentOrderId(),
                request.getReason());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/toss/prepare")
    @Operation(summary = "Toss 테스트 결제 준비", description = "장바구니를 토스 결제 대기 주문으로 전환하고 재고를 예약합니다.")
    public ResponseEntity<?> prepareTossPayment(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        PaymentDto payment = paymentService.prepareTossPayment(principalDetails.getUser());
        return ResponseEntity.status(HttpStatus.CREATED).body(payment);
    }

    @PostMapping("/toss/confirm")
    @Operation(summary = "Toss 테스트 결제 승인", description = "토스 결제 인증 결과를 서버에서 검증하고 결제 승인 API를 호출합니다.")
    public ResponseEntity<?> confirmTossPayment(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                                @Valid @RequestBody TossPaymentConfirmRequest request) {
        if (principalDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        PaymentDto payment = paymentService.confirmTossPayment(
                principalDetails.getUser(),
                request.getPaymentKey(),
                request.getOrderId(),
                request.getAmount());
        return ResponseEntity.ok(payment);
    }

    @PostMapping("/toss/fail")
    @Operation(summary = "Toss 테스트 결제 실패", description = "토스 결제창 실패 또는 취소 결과를 저장하고 예약 재고를 복구합니다.")
    public ResponseEntity<?> failTossPayment(@AuthenticationPrincipal PrincipalDetails principalDetails,
                                             @Valid @RequestBody PaymentFailRequest request) {
        if (principalDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("로그인이 필요합니다.");
        }

        PaymentDto payment = paymentService.failTossPayment(
                principalDetails.getUser(),
                request.getPaymentOrderId(),
                request.getReason());
        return ResponseEntity.ok(payment);
    }
}
