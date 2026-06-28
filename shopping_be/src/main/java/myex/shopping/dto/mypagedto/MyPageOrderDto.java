package myex.shopping.dto.mypagedto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.Order;
import myex.shopping.domain.OrderStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Schema(description = "마이페이지 보낼 주문 DTO")
public class MyPageOrderDto {
    @Schema(description = "마이페이지 주문ID", example = "1")
    private Long id;
//    user 나중에 고민.(마이 페이지라 세션에서 꺼내면 될꺼 같기도)
    @Schema(description = "주문 전체 가격", example = "6000")
    private Integer totalPrice;//메소드 미리 받아둠.
    @Schema(description = "주문 전체 수량", example = "6")
    private Integer totalQuantity;
    @Schema(description = "주문 상태", example = "ORDERED, CANCELLED")
    private OrderStatus status;
    @Schema(description = "주문 일자", example = "2025-10-27")
    private LocalDateTime orderDate;
    @Schema(description = "주문 상품들", example = "[orderitem1, orderitem2]")
    private List<MyPageOrderItemDto> orderItems; //OrderItem을 표현할 DTO

    @Schema(description = "주문이 취소가 아닐때(SSR에서 CANCELLED시 취소 버튼 사라지게 설정",example = "true, false")
    private boolean orderIsNotCanCelled;

    public MyPageOrderDto(Order order) {
        this.id = order.getId();
        this.totalPrice = order.getTotalPrice();
        this.totalQuantity = order.getTotalQuantity();
        this.status = order.getStatus();
        this.orderDate = order.getOrderDate();
        this.orderItems = order.getOrderItems().stream() //LAZY 초기화
                .map(MyPageOrderItemDto::new)
                .collect(Collectors.toList());

        this.orderIsNotCanCelled = order.orderIsNotCanCelled();
    }

    @Override
    public String toString() {
        return "MyPageOrderDto{" +
                "id=" + id +
                ", totalPrice=" + totalPrice +
                ", totalQuantity=" + totalQuantity +
                ", status=" + status +
                ", orderDate=" + orderDate +
                ", orderItems=" + orderItems +
                '}';
    }
}
