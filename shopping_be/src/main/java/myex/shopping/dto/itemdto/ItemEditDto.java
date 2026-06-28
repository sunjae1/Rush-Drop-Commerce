package myex.shopping.dto.itemdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import myex.shopping.domain.Item;
import org.springframework.format.annotation.NumberFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Schema(description = "상품 수정 정보 담는 DTO")
public class ItemEditDto {
    @Schema(description = "상품ID", example = "1")
    private Long id;
    @Schema(description = "상품이름", example = "아이템A")
    private String itemName;
    @Schema(description = "상품가격", example = "2000")
    @NumberFormat(pattern = "#,###")
    private int price;
    @Schema(description = "상품재고 수량", example = "30")
    private int quantity;
    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;
    @Schema(description = "상품 이미지 URL (Pre-signed URL)", example = "https://bucket.s3.amazonaws.com/images/1.webp?X-Amz-...")
    @Setter
    private String imageUrl;
    @Schema(description = "상품 파일(바이너리 데이터)")
    private MultipartFile imageFile;
    @Schema(description = "드롭 상품 여부", example = "true")
    private boolean dropProduct;
    @Schema(description = "드롭 판매 시작 시간", example = "2026-07-01T20:00:00")
    private LocalDateTime dropStartsAt;
    @Schema(description = "드롭 판매 종료 시간", example = "2026-07-01T21:00:00")
    private LocalDateTime dropEndsAt;
    @Schema(description = "드롭 상품 1인 구매 제한 수량", example = "1")
    private Integer dropPurchaseLimit;
    @Schema(description = "드롭 판매 상태", example = "UPCOMING")
    private String dropSaleStatus;

    public ItemEditDto(Item item) {
        this.id = item.getId();
        this.itemName = item.getItemName();
        this.price = item.getPrice();
        this.quantity = item.getQuantity();
        this.categoryId = item.getCategory() != null ? item.getCategory().getId() : null;
        this.imageUrl = item.getImageUrl();
        this.imageFile = item.getImageFile();
        this.dropProduct = item.isDropProduct();
        this.dropStartsAt = item.getDropStartsAt();
        this.dropEndsAt = item.getDropEndsAt();
        this.dropPurchaseLimit = item.getDropPurchaseLimit();
        this.dropSaleStatus = item.resolveDropSaleStatus(LocalDateTime.now()).name();
    }
}
