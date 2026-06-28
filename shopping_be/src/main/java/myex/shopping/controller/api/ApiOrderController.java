package myex.shopping.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import myex.shopping.dto.userdto.PrincipalDetails;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Cart;
import myex.shopping.domain.User;
import myex.shopping.dto.orderdto.OrderDBDto;
import myex.shopping.dto.orderdto.OrderDto;
import myex.shopping.service.CartService;
import myex.shopping.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Tag(name = "Order", description = "주문 관련 API")
@Validated
public class ApiOrderController {
        private final CartService cartService;
        private final OrderService orderService;

        @Operation(summary = "장바구니를 주문으로 전환", description = "장바구니 전체 아이템을 하나의 주문으로 전환합니다.", responses = {
                        @ApiResponse(responseCode = "201", description = "주문 생성 완료"),
                        @ApiResponse(responseCode = "401", description = "로그인 실패"),
                        @ApiResponse(responseCode = "400", description = "클라이언트 오류")
        })
        // 장바구니 전체 -> 주문으로 전환.
        @PostMapping
        public ResponseEntity<?> order_change(@AuthenticationPrincipal PrincipalDetails principalDetails) {
                // 주문 실패 로직.
                if (principalDetails == null) {
                        log.info("로그인 실패");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body("로그인이 필요합니다.");
                }
                User loginUser = principalDetails.getUser();
                // 장바구니가 아무것도 없을때 주문하기 눌르면,
                Cart cart = cartService.findOrCreateCartForUser(loginUser);
                if (cart == null || cart.getCartItems().isEmpty()) {
                        log.info("주문 불가 : 장바구니 EMPTY");
                        return ResponseEntity.badRequest()
                                        .body("주문 불가 : 장바구니에 상품을 담아주세요.");
                }
                // 주문 성공 로직.
                // Order 생성
                // OrderItem 생성 : 장바구니를 주문으로 전환.
                // Cart : CartItem ==> Order : OrderItem 전환.
                OrderDto orderDto = new OrderDto(orderService.checkout(loginUser));
                // 생성 완료. /주문 완료된 order 보여줌.

                // 장바구니 전체 삭제.
                cartService.deleteCart(cart.getId(), loginUser.getId());

                return ResponseEntity.status(HttpStatus.CREATED).body(orderDto);
        }

        @Operation(summary = "전체 주문 조회", description = "관리자가 전체 주문을 조회합니다.", responses = {
                        @ApiResponse(responseCode = "200", description = "조회 성공")
        })
        // 전체 주문 조회
        @GetMapping
        public ResponseEntity<List<OrderDBDto>> orderAll() {
                List<OrderDBDto> orderAll = orderService.findAllOrderDtos();
                return ResponseEntity.ok(orderAll);
        }

        @Operation(summary = "주문 취소", description = "사용자의 주문을 취소합니다.", responses = {
                        @ApiResponse(responseCode = "200", description = "조회 성공"),
                        @ApiResponse(responseCode = "404", description = "주문 조회 실패")
        })
        // 주문 취소. : items/{id}/cancel
        @DeleteMapping("/{id}")
        public ResponseEntity<?> orderCancel(@PathVariable @Positive Long id,
                        @AuthenticationPrincipal PrincipalDetails principalDetails) {
                if (principalDetails == null) {
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                        .body("로그인이 필요합니다.");
                }
                User loginUser = principalDetails.getUser();
                OrderDto orderDto = new OrderDto(orderService.orderCancel(id, loginUser));
                return ResponseEntity.ok(orderDto);
        }
}
