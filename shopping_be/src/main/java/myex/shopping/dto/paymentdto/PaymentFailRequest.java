package myex.shopping.dto.paymentdto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentFailRequest {
    @NotBlank(message = "결제 주문번호가 필요합니다.")
    private String paymentOrderId;

    @Size(max = 255, message = "실패 사유는 255자 이하로 입력해주세요.")
    private String reason;
}
