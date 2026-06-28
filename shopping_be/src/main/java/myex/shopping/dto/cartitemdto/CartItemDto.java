package myex.shopping.dto.cartitemdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import myex.shopping.domain.CartItem;
import myex.shopping.dto.itemdto.ItemDto;

@Getter
@Setter
@Schema(description = "장바구니 상품 정보 담는 DTO")
public class CartItemDto {
    @Schema(description = "상품 정보 담는 DTO", example = "itemDTO")
    private ItemDto item;
    @Schema(description = "장바구니 담은 상품 수량", example = "3")
    private int quantity;

    public CartItemDto(CartItem cartItem) {
        this.quantity = cartItem.getQuantity();
        this.item = new ItemDto(cartItem.getItem());
    }
    //총 가격 : 1000 * 3 = 3000;
    public int totalItemPrice() {
        if (item != null) {
            return item.getPrice() * quantity;
        }
        return 0;
    }
}
