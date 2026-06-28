package myex.shopping.controller.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Cart;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.dto.cartdto.CartDto;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.dto.userdto.UserDto;
import myex.shopping.exception.ResourceNotFoundException;
import myex.shopping.form.CartForm;
import myex.shopping.repository.ItemRepository;
import myex.shopping.service.CartService;
import myex.shopping.service.ImageService;
import myex.shopping.service.ItemService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/items")
public class CartController {
    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final CartService cartService;
    private final ImageService imageService;

    // 한 상품에 대한 주문 페이지를 보여주고, 장바구니 담기 클릭시 장바구니에 저장.
    @GetMapping("/{itemId}/cart")
    public String viewCart(@PathVariable Long itemId,
                           @ModelAttribute CartForm cartForm,
                           @AuthenticationPrincipal PrincipalDetails principalDetails,
                           Model model) {
        if (principalDetails != null) {
            User loginUser = principalDetails.getUser();
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        ItemDto item = itemService.findByIdToDto(itemId);
        model.addAttribute("item", item);
        return "cart/cartForm";
    }

    // 한 상품에 대한 주문 페이지에서 정보가 넘어오면 장바구니에 저장.
    @PostMapping("/{itemId}/cart")
    public String addToCart(@Valid @ModelAttribute CartForm cartForm,
                            BindingResult bindingResult,
                            Model model,
                            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        log.info("cart 상품 추가 컨트롤러 진입");
        log.info("cartForm 정보 : {}", cartForm);

        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        if (loginUser != null) {
            UserDto userDto = new UserDto(loginUser);
            model.addAttribute("user", userDto);
        }
        Item item = itemRepository.findById(cartForm.getId())
                .orElseThrow(() -> new ResourceNotFoundException("item not found"));
        if (bindingResult.hasErrors()) {
            log.info("검증 실패 : {}", bindingResult);
            model.addAttribute("item", imageService.resolveImageUrl(new ItemDto(item)));
            return "cart/cartForm";
        }
        // 재고 수량 초과로 장바구니 담을 시
        if (item.getQuantity() < cartForm.getQuantity()) {
            log.info("장바구니 담기 : 재고 수량 초과 오류");
            bindingResult.rejectValue("quantity", "Exceed", "상품 재고 수량을 초과할 수 없습니다.");
            model.addAttribute("item", imageService.resolveImageUrl(new ItemDto(item)));
            return "cart/cartForm";
        }
        cartService.addItem(loginUser, item.getId(), cartForm.getQuantity());
        return "redirect:/";
    }

    // 장바구니 전체 보여주는 뷰.
    @GetMapping("/cartAll")
    public String cartAll(Model model,
                          @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;

        CartDto cart = cartService.findByUserByDto(loginUser);
        model.addAttribute("cart", cart);
        return "cart/cart_view";
    }

    // 장바구니 아이템 삭제
    @PostMapping("/cart/remove")
    public String cartItemRemove(@RequestParam Long itemId,
                                 @AuthenticationPrincipal PrincipalDetails principalDetails) {
        User loginUser = principalDetails != null ? principalDetails.getUser() : null;
        cartService.deleteItem(itemId, loginUser);
        return "redirect:/items/cartAll";
    }

}
