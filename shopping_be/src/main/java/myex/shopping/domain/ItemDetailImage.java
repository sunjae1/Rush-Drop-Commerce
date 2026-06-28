package myex.shopping.domain;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "item_detail_image",
        indexes = {
                @Index(name = "idx_item_detail_image_item_order", columnList = "item_id, display_order")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_item_detail_image_item_order", columnNames = {"item_id", "display_order"})
        }
)
public class ItemDetailImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_role", nullable = false, length = 30)
    private ItemDetailImageRole imageRole;

    @Column(name = "image_url", nullable = false, length = 1024)
    private String imageUrl;

    @Column(name = "alt_text", nullable = false)
    private String altText;

    @Column(length = 255)
    private String caption;

    protected ItemDetailImage() {
    }

    public ItemDetailImage(int displayOrder,
                           ItemDetailImageRole imageRole,
                           String imageUrl,
                           String altText,
                           String caption) {
        this.displayOrder = displayOrder;
        this.imageRole = imageRole;
        this.imageUrl = imageUrl;
        this.altText = altText;
        this.caption = caption;
    }

    public void changeItem(Item item) {
        if (this.item != null) {
            this.item.getDetailImages().removeIf(existingImage -> existingImage == this);
        }

        this.item = item;

        if (item != null && item.getDetailImages().stream().noneMatch(existingImage -> existingImage == this)) {
            item.getDetailImages().add(this);
        }
    }
}
