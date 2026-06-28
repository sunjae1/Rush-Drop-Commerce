package myex.shopping.dto.orderdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.Order;
import myex.shopping.domain.OrderStatus;
import myex.shopping.dto.mypagedto.MyPageOrderItemDto;
import myex.shopping.dto.userdto.UserDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Schema(description = "DB에서 꺼낸 주문 정보 담는 DTO")
public class OrderDBDto {
    @Schema(description = "DB 주문 DTO ID", example = "1")
    private Long id;
//    user 나중에 고민.(마이 페이지라 세션에서 꺼내면 될꺼 같기도)
    @Schema(description = "주문한 사용자", example = "user")
    private UserDto user;
    @Schema(description = "주문 합산 전체 가격", example = "6000")
    private Integer totalPrice;//메소드 미리 받아둠.
    @Schema(description = "주문 전체 수량", example = "6")
    private Integer totalQuantity;
    @Schema(description = "주문 상태", example = "ORDERED, CANCELLED")
    private OrderStatus status;
    @Schema(description = "주문 일자", example = "2025-10-27")
    private LocalDateTime orderDate;
    @Schema(description = "주문 상품들", example = "[orderItem1, orderItem2]")
    private List<MyPageOrderItemDto> orderItems; //OrderItem을 표현할 DTO

    @Schema(description = "주문 취소 확인 필드", example = "true, false")
    private boolean orderIsNotCanCelled;

    public OrderDBDto(Order order) {
        this.id = order.getId();

        this.user = new UserDto(order.getUser());

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
        return "OrderDBDto{" +
                "id=" + id +
                ", user=" + user +
                ", totalPrice=" + totalPrice +
                ", totalQuantity=" + totalQuantity +
                ", status=" + status +
                ", orderDate=" + orderDate +
                '}';
    }
}
