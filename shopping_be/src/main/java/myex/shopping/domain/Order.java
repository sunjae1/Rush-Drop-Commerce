package myex.shopping.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
@Entity
@Table(name = "orders") //ORDER BY 같은 예약어 때문에 충돌.
//프로퍼티 접근법 Thymeleaf 에서 쓰임.
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    //CascadeType.ALL (엔티티, DB 관점 삭제 따라감)
    //Parent.remove()  --> Child.delete()
    //orphanRemoval=true (자바 컬렉션 관점 삭제 더티 체킹으로 따라감)
    //Parent.getChildren().remove(child)  --> Child.delete()
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();
    private LocalDateTime orderDate;
   @Enumerated(EnumType.STRING)
    private OrderStatus status;

    //JPA 전용 기본 생성자
    protected Order() {
    }

    //주문 시.
    public Order(User user) {
        this.user = user;
        this.orderDate = LocalDateTime.now();
        this.status = OrderStatus.ORDERED;
    }

    //주문 아이템 추가 --> 연관관계 편의 메소드 (객체 그래프 동기화)
    public void addOrderItem(OrderItem orderItem) {
        orderItems.add(orderItem);
        orderItem.setOrder(this);
    }

    //총 금액 계산
    public int getTotalPrice() {
        //OrderItem 스트림을 int로 바꾸는 (OrderItem) -> int 인 function을 요구.
        return orderItems.stream()
                .mapToInt(OrderItem::getTotalPrice)
                .sum();
    }

    //총 수량 계산
    public int getTotalQuantity() {
        return orderItems.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();
    }



    //주문 확정 시 재고 감소 : 주문 --> 결제 --> 확정(orderItem이 item을 가져야만 decreaseStock() 사용가능.) 앞 사람이 재고를 다 털어가면 Exception 내야함.
    public void confirmOrder() {
        for (OrderItem orderItem : orderItems) {
            //다른 사람이 먼저 주문해서 수량이 없을 시 -> false로 주문 불가 거절.
            orderItem.getItem().decreaseStock(orderItem.getQuantity());

        }
        this.status =OrderStatus.PAID; //주문 체결.
    }

    public void reserveStockForPayment() {
        if (this.status != OrderStatus.ORDERED) {
            throw new IllegalStateException("결제 대기로 변경할 수 없는 주문 상태입니다.");
        }
        for (OrderItem orderItem : orderItems) {
            orderItem.getItem().decreaseStock(orderItem.getQuantity());
        }
        this.status = OrderStatus.PAYMENT_PENDING;
    }

    public void completePayment() {
        if (this.status == OrderStatus.PAID) {
            return;
        }
        if (this.status != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 완료로 변경할 수 없는 주문 상태입니다.");
        }
        this.status = OrderStatus.PAID;
    }

    public void failPaymentAndReleaseStock() {
        if (this.status == OrderStatus.PAYMENT_FAILED) {
            return;
        }
        if (this.status != OrderStatus.PAYMENT_PENDING) {
            throw new IllegalStateException("결제 실패로 변경할 수 없는 주문 상태입니다.");
        }
        for (OrderItem orderItem : orderItems) {
            orderItem.getItem().increaseStock(orderItem.getQuantity());
        }
        this.status = OrderStatus.PAYMENT_FAILED;
    }

    //주문 취소
    public void cancel() {
        if (this.status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("이미 취소된 주문입니다.");
        }
        if (this.status == OrderStatus.PAYMENT_FAILED) {
            throw new IllegalStateException("결제 실패 주문은 취소할 수 없습니다.");
        }
        if (this.status == OrderStatus.PAID || this.status == OrderStatus.PAYMENT_PENDING) {
            for (OrderItem orderItem : orderItems) {
                orderItem.getItem().increaseStock(orderItem.getQuantity());
            }
        }
        this.status =OrderStatus.CANCELLED;
    }


    //전체 장바구니 --> 주문 버튼으로.
    //장바구니 -> Order 로 전환
    public Order checkout(Order order, Cart cart, User user) {
        for (CartItem ci : cart.getCartItems()) {
            order.addOrderItem(new OrderItem(ci.getItem(), ci.getItem().getPrice(), ci.getQuantity()));
        }
        return order;
    }

    public boolean orderIsNotCanCelled() {

        if (this.status ==OrderStatus.CANCELLED || this.status == OrderStatus.PAYMENT_FAILED)
        {
            return false;
        }
        else {
            return true;
        }
    }



}
