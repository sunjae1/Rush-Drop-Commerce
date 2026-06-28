package myex.shopping.dto.cartdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import myex.shopping.domain.Cart;
import myex.shopping.domain.CartItem;
import myex.shopping.dto.cartitemdto.CartItemDto;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Schema(description = "장바구니 정보 담는 DTO")
public class CartDto {
    @Schema(description = "장바구니 상품 담는 DTO", example = "[CartItem1, CartItem2]")
    private List<CartItemDto> cartItems;
    @Schema(description = "장바구니 전체 아이템 가격", example = "6000")
    //장바구니 모든 아이템 가격 출력
    private Integer allPrice;

    public CartDto() {
    }

    public CartDto(List<CartItemDto> cartItems) {
        this.cartItems = cartItems;

    }

    public CartDto(Cart cart) {
        this.allPrice = cart.allPrice();
        this.cartItems = cart.getCartItems().stream()
                .map(CartItemDto::new)
                .collect(Collectors.toList());
    }

    public int allPrice() {
        int allPrice=0;
        for (CartItemDto ci : cartItems) {
            allPrice +=ci.totalItemPrice();
        }
        return allPrice;
    }
}
