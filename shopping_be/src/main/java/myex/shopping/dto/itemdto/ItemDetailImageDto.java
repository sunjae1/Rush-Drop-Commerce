package myex.shopping.dto.itemdto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import myex.shopping.domain.ItemDetailImage;

@Getter
@Schema(description = "상품 상세 페이지용 이미지")
public class ItemDetailImageDto {
    @Schema(description = "상세 이미지 ID", example = "1")
    private Long id;
    @Schema(description = "상품 상세 화면 노출 순서", example = "1")
    private int displayOrder;
    @Schema(description = "상세 이미지 역할", example = "MOOD")
    private String imageRole;
    @Schema(description = "상세 이미지 URL", example = "https://images.pexels.com/photos/example.jpeg")
    @Setter
    private String imageUrl;
    @Schema(description = "이미지 대체 텍스트", example = "스니커즈 착용 무드 컷")
    private String altText;
    @Schema(description = "이미지 캡션", example = "착용 컷")
    private String caption;

    public ItemDetailImageDto(ItemDetailImage detailImage) {
        this.id = detailImage.getId();
        this.displayOrder = detailImage.getDisplayOrder();
        this.imageRole = detailImage.getImageRole().name();
        this.imageUrl = detailImage.getImageUrl();
        this.altText = detailImage.getAltText();
        this.caption = detailImage.getCaption();
    }
}
