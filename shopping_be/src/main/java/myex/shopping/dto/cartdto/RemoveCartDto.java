package myex.shopping.dto.cartdto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "삭제할 장바구니 DTO")
public class RemoveCartDto {

    @NotNull(message = "Null 허용 불가.")
    @Positive(message = "양수만 입력가능합니다.")
    @Schema(description = "상품ID", example = "1")
    private Long itemId;
}
