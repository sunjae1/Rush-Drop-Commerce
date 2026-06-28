package myex.shopping.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import myex.shopping.exception.InsufficientStockException;
import org.hibernate.annotations.SQLDelete;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//재고 입장 아이템.
//@Data
@Getter
@Setter
@Entity
@SQLDelete(sql = "UPDATE item SET deleted = true WHERE id = ?")
public class Item {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String itemName;
    private Integer price; //가격
    private Integer quantity; //수량(남은재고)
    private boolean dropProduct = false;
    private LocalDateTime dropStartsAt;
    private LocalDateTime dropEndsAt;
    private Integer dropPurchaseLimit;

    @Transient //JPA가 컬럼으로 만들지 않음 :업로드 파일은 서버에 저장/DB에는 경로만 저장. 지금은 Form -> url로 저장함.
    private MultipartFile imageFile; //업로드 파일
    private String imageUrl; //이미지 경로

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ItemDetailImage> detailImages = new ArrayList<>();

    private boolean deleted = false; // 초기값 false.(주문내역 있을 시 상태 바꿈으로 아이템 숨김 처리)


    public Item() {
    }

    public Item(String itemName, Integer price, Integer quantity, String imageUrl) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    public Item(String itemName, Integer price, Integer quantity) {
        this.itemName = itemName;
        this.price = price;
        this.quantity = quantity;
    }

    public void changeCategory(Category category) {
        if (this.category != null) {
            this.category.getItems().removeIf(existingItem -> existingItem == this);
        }

        this.category = category;

        if (category != null) {
            boolean alreadyMapped = category.getItems().stream()
                    .anyMatch(existingItem -> existingItem == this);
            if (!alreadyMapped) {
                category.getItems().add(this);
            }
        }
    }

    public void addDetailImage(ItemDetailImage detailImage) {
        if (detailImage == null) {
            return;
        }
        detailImage.changeItem(this);
    }

    public void clearDetailImages() {
        List.copyOf(detailImages).forEach(detailImage -> detailImage.changeItem(null));
        detailImages.clear();
    }

    public void decreaseStock(int quantity) {
        int decreasedQuantity = this.quantity - quantity;
        if (decreasedQuantity <0)
            throw new InsufficientStockException("재고가 부족합니다.");
        this.quantity = decreasedQuantity;
    }

    public void increaseStock(int quantity) {
        this.quantity += quantity;
    }

    public void configureDropSale(boolean dropProduct,
                                  LocalDateTime dropStartsAt,
                                  LocalDateTime dropEndsAt,
                                  Integer dropPurchaseLimit) {
        this.dropProduct = dropProduct;
        if (!dropProduct) {
            this.dropStartsAt = null;
            this.dropEndsAt = null;
            this.dropPurchaseLimit = null;
            return;
        }

        this.dropStartsAt = dropStartsAt;
        this.dropEndsAt = dropEndsAt;
        this.dropPurchaseLimit = dropPurchaseLimit;
    }

    public DropSaleStatus resolveDropSaleStatus(LocalDateTime now) {
        if (!dropProduct) {
            return DropSaleStatus.STANDARD;
        }

        if (dropStartsAt != null && now.isBefore(dropStartsAt)) {
            return DropSaleStatus.UPCOMING;
        }

        if (dropEndsAt != null && !now.isBefore(dropEndsAt)) {
            return DropSaleStatus.ENDED;
        }

        return DropSaleStatus.LIVE;
    }


    @Override
    public String toString() {
        return "선택된 상품 ={'" + itemName + '\'' +
                ", 가격=" + price +
                ", 수량=" + quantity +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return Objects.equals(getId(), item.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }
}
