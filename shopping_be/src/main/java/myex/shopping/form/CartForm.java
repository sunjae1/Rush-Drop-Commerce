package myex.shopping.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

//@Data
@Getter
@Setter
@Schema(description = "장바구니 정보 담는 FORM")
public class CartForm {

    //주문 시 받을 정보 : itemId(아이템 조회), 수량 (몇개 주문 했는지)
    @NotNull(message = "id는 null 일 수 없습니다.")
    @Schema(description = "상품 ID", example = "1")
    private Long id;

    @NotNull(message = "수량을 입력해주세요")
    @Min(value = 1, message = "수량은 1부터 입력 가능합니다.")
    @Schema(description = "장바구니 담을 상품 수량", example = "3")
    private Integer quantity;

    //price (장바구니 BindingResult 뿌리기 위해서)
    @Schema(description = "장바구니 담을 시 가격", example = "2000")
    private Integer price;

    @Override
    public String toString() {
        return "CartForm{" +
                "id=" + id +
                ", quantity=" + quantity +
                ", price=" + price +
                '}';
    }
}
