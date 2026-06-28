package myex.shopping.dto.paymentdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentConfirmRequest {
    @NotBlank(message = "결제 주문번호가 필요합니다.")
    private String paymentOrderId;

    @Positive(message = "결제 금액은 양수여야 합니다.")
    private int amount;
}
