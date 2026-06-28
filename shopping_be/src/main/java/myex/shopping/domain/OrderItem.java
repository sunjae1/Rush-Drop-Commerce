package myex.shopping.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

//@Data
//--> equals(), hashCode() 메소드 자동 생성하는데, JPA 지연 로딩 과 복잡한 연관관계 맺을 때 예상치 못한 문제를 일으킬 수 있다.
@Getter
@Setter
@Entity
public class OrderItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id")
    private Item item; //주문 생성시점 주문 확정 시점이 다를 때는 참조하고 있어야 편리 할인/재고 차감 같은 것이 결제 완료 후 처리 로직으로 바로 적용 가능 하니깐.
    private int orderPrice; //주문 당시 가격
    private int quantity;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    protected OrderItem() {
    }

    //책임을 정의만 하고 흐름은 더 위에서 활용.
    //메소드를 짤때, 안에서만 신경써서 기능을 만들면 될듯 하다. 그 기능을 쓰는건 외부에서
    //(메소드 : 도구를 만들어놓고, 도구 활용은 외부에서 활용.)
    public int getTotalPrice() {
        return orderPrice * quantity;
    }

    public OrderItem(Item item, int orderPrice, int quantity) {
        this.item = item;
        this.orderPrice = orderPrice;
        this.quantity = quantity;
    }

    //계산식.
    public String printCalculate() {
        return orderPrice + " X " + quantity + "= "+getTotalPrice();
    }

    @Override
    public String toString() {
        return "주문상품 : " +
                item + "<br>" +
                "주문가격=" + orderPrice + "<br>" +
                "주문수량=" + quantity;
    }
}
