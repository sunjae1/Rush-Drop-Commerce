package myex.shopping.controller.web;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.*;
import myex.shopping.dto.orderdto.OrderDBDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.service.CartService;
import myex.shopping.service.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/items")
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    // 장바구니 전체 -> 주문으로 변환.
    @PostMapping("/order")
    public String order_change(Model model,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User loginUser = principalDetails.getUser();
        Cart cart = cartService.findOrCreateCartForUser(loginUser);
        if (cart == null || cart.getCartItems().isEmpty()) {
            log.info("주문 실패 로직 : 장바구니에 아무것도 없을 때");
            model.addAttribute("empty_cart_error", "주문 불가 : 장바구니에 상품을 담아주세요.");
            return "cart/cart_view";
        }
        log.info("장바구니 -> 주문 전환 전 : checkout 전");
        Order checkout = orderService.checkout(loginUser);
        log.info("장바구니 -> 주문 전환 후 : order 정보 {} ", checkout);
        cartService.deleteCart(cart.getId(), loginUser.getId());
        return "redirect:/";
    }

    // 주문 전체 조회.
    @GetMapping("/orderAll")
    public String orderAll(Model model,
            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        List<OrderDBDto> orderAll = orderService.findAllOrderDtos();
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        log.info("orderDTO List 정보 : {}", orderAll);
        model.addAttribute("orders", orderAll);
        model.addAttribute("loginUser", loginUser);
        return "order/order_view";
    }

    // 주문 취소. : items/{id}/cancel
    @PostMapping("/{id}/cancel")
    public String orderCancel(@PathVariable Long id,
            @AuthenticationPrincipal PrincipalDetails principalDetails,
            @RequestParam(required = false) String redirectInfo) {
        User loginUser = principalDetails.getUser();
        orderService.orderCancel(id, loginUser);
        if ("mypage".equals(redirectInfo)) {
            return "redirect:/mypage";
        }
        return "redirect:/items/orderAll";
    }
}
