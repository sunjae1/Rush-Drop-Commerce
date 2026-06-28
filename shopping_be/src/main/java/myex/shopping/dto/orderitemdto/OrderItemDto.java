package myex.shopping.dto.orderitemdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.OrderItem;

@Getter
@Schema(description = "주문상품을 담는 DTO")
public class OrderItemDto {
    @Schema(description = "선택 상품이름", example = "아이템A")
    private String itemName;
    @Schema(description = "주문 시 가격", example = "2000")
    private int price;
    @Schema(description = "주문 개수", example = "3")
    private int quantity;

    public OrderItemDto(OrderItem oi) {
        this.itemName = oi.getItem().getItemName();
        this.price = oi.getItem().getPrice();
        this.quantity = oi.getQuantity();
    }
}
