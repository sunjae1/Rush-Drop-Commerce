package myex.shopping.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

//@Data
@Getter
@Setter
@Entity

@ToString(exclude = "cart")
public class CartItem {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //아이템과 수량만 기록
    //다대일 : 각각 Cart 에서 다른 CartItem이 같은 Item을 가질 수 있다.
    //관계 유형은 **DB 전체 관점**에서 결정
    /* 예시.
    1 Cart 1 CartItem 1Item(사과)
    2 Cart 2 CartItem 1Item(사과)
    */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_id") //FK "다" 에 저장.
    private Item item;
    private int quantity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private Cart cart;

    public CartItem() {
    }

    public CartItem(Item item, int quantity) {
        this.item = item;
        this.quantity = quantity;
    }

    public CartItem(Item item) {
        this.item = item;
    }

    public void addQuantity(int quantity) {
        this.quantity += quantity;
    }

    public int totalItemPrice() {
        return item.getPrice() * quantity;
    }


}
