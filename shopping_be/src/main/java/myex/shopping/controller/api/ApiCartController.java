package myex.shopping.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.domain.Cart;
import myex.shopping.domain.Item;
import myex.shopping.domain.User;
import myex.shopping.dto.cartdto.CartDto;
import myex.shopping.dto.userdto.PrincipalDetails;
import myex.shopping.form.CartForm;
import myex.shopping.repository.CartRepository;
import myex.shopping.repository.ItemRepository;
import myex.shopping.service.CartService;
import myex.shopping.service.ImageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Optional;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "장바구니 관련 API")
@Validated
public class ApiCartController {
    private final ItemRepository itemRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;
    private final ImageService imageService;

    @Operation(summary = "선택 상품 장바구니 저장", description = "한 상품에 대해서 선택 시 장바구니에 저장", responses = {
            @ApiResponse(responseCode = "200", description = "장바구니 추가 성공"),
            @ApiResponse(responseCode = "400", description = "클라이언트 오류(재고 수량 초과)"),
            @ApiResponse(responseCode = "401", description = "로그인 실패"),
            @ApiResponse(responseCode = "404", description = "없는 상품 장바구니 추가")
    })
    // 한 상품에 대한 주문 페이지에서 정보가 넘어오면 장바구니에 저장.
    // cartForm 에서 id - item.id 매핑됨.
    // 입력값 : id, price, quantity --> 매핑값 : CartForm : id(itemId), quantity 매핑.
    // (2개 필드 매핑)
    // itemId나 CartForm id 중 하나만 쓰기. --> itemId로.
    @PostMapping("/items/{itemId}")
    public ResponseEntity<CartDto> addToCart(@PathVariable @Positive(message = "양수만 입력가능합니다.") Long itemId,
                                             @Valid @RequestBody CartForm cartForm,
                                             @AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            log.info("로그인 실패");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();// 401
        }
        User loginUser = principalDetails.getUser();
        // 많은 작업 -> 변수 추출 /Stream() -> 더 복잡.
        Optional<Item> findItemOpt = itemRepository.findById(itemId);
        if (findItemOpt.isEmpty()) {
            return ResponseEntity.notFound().build(); // 없는 상품을 추가 시 404
        }
        Item findItem = findItemOpt.get();
        // 재고 수량 초과로 장바구니 담을 시
        if (findItem.getQuantity() < cartForm.getQuantity()) {
            log.info("재고 수량 초과로 장바구니 담을 시");
            return ResponseEntity.badRequest().build(); // 클라이언트 오류 400
        }
        Cart cart = cartService.addItem(loginUser, findItem.getId(), cartForm.getQuantity());
        CartDto cartDto = new CartDto(cart);
        imageService.resolveCartItemImageUrls(cartDto.getCartItems());
        return ResponseEntity.ok(cartDto); // 200
    }

    @Operation(summary = "장바구니 전체 조회", description = "한 사용자의 장바구니 전체를 조회합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "해당 유저 장바구니를 못 찾음.")
    })
    // 장바구니 전체 보여주는 뷰. cartItem List 전달.
    @GetMapping
    public ResponseEntity<CartDto> cartAll(@AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        User loginUser = principalDetails.getUser();

        return cartRepository.findByUser(loginUser)
                .map(cart -> {
                    CartDto dto = new CartDto(cart);
                    imageService.resolveCartItemImageUrls(dto.getCartItems());
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.ok(new CartDto(Collections.emptyList()))); // null 해야 빈 장바구니 출력 가능.
    }

    @Operation(summary = "장바구니 상품 하나 삭제", description = "전체 장바구니에서 상품 하나를 삭제합니다.", responses = {
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "삭제 상품을, 해당 사용자의 장바구니를 못찾았을때")
    })
    // 장바구니 아이템 삭제
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<?> cartItemRemove(@PathVariable @Positive Long itemId,
                                            @AuthenticationPrincipal PrincipalDetails principalDetails) {
        if (principalDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        User loginUser = principalDetails.getUser();
        Optional<Item> findItemOpt = itemRepository.findById(itemId);
        if (findItemOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Item findItem = findItemOpt.get();
        Cart cart = cartService.deleteItem(findItem.getId(), loginUser);
        CartDto dto = new CartDto(cart);
        imageService.resolveCartItemImageUrls(dto.getCartItems());
        return ResponseEntity.ok(dto);
    }
}
