package myex.shopping.dto.orderdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import myex.shopping.domain.Order;
import myex.shopping.dto.orderitemdto.OrderItemDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Schema(description = "주문을 담는 DTO")
public class OrderDto {
    @Schema(description = "주문 ID", example = "1")
    private Long id;
    @Schema(description = "주문 상품들", example = "[orderItemDTO1, orderItemDTO2]")
    private List<OrderItemDto> orderItems;
    @Schema(description = "주문일자", example = "2025-10-27")
    private LocalDateTime orderDate;
    @Schema(description = "주문 상태", example = "ORDERED, CANCELLED")
    private String status;

    public OrderDto(Order order) {
        this.id = order.getId();
        this.orderDate = order.getOrderDate();
        this.status = order.getStatus().name();
        this.orderItems = order.getOrderItems().stream()
                .map(OrderItemDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return "OrderDto{" +
                "id=" + id +
                ", orderItems=" + orderItems +
                ", orderDate=" + orderDate +
                ", status='" + status + '\'' +
                '}';
    }
}
