package myex.shopping.dto.mypagedto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.OrderItem;

@Getter
@Schema(description = "마이 페이지 보낼 주문 상품 정보 담는 DTO")
public class MyPageOrderItemDto {

    @Schema(description = "주문상품 ID",example = "1")
    private Long id;
    @Schema(description = "주문 상품 이름", example = "아이템A")
    private String itemName;
    @Schema(description = "주문 시 가격", example = "2000")
    private Integer orderPrice;
    @Schema(description = "주문 수량", example = "3")
    private Integer quantity;
    @Schema(description = "계산식 담는 필드", example = "2000X3 = 6000")
    private String calculation; //printCalculate() 결과 담을 필드.

    @Schema(description = "상품 남은 재고(확인용)", example = "13")
    private Integer itemStock; //남은 재고 (내가 볼려고)


    public MyPageOrderItemDto(OrderItem orderItem) {
        this.id = orderItem.getId();
        this.itemName = orderItem.getItem().getItemName(); //LAZY 초기화.
        this.orderPrice = orderItem.getOrderPrice();
        this.quantity = orderItem.getQuantity();
        this.calculation = orderItem.printCalculate();

        this.itemStock = orderItem.getItem().getQuantity();
    }

    @Override
    public String toString() {
        return "주문 상품 : <br>" +
                "선택된 상품 ='" + itemName + '\'' +
                ", 주문 가격 = " + orderPrice +
                ", 주문 수량 =" + quantity +
                ", 상품 남은 재고량 = " + itemStock +
                '}';
    }
}
