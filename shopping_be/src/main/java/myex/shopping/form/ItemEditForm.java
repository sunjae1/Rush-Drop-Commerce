package myex.shopping.form;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Range;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Getter
@Setter
@Schema(description = "상품수정 FORM")
public class ItemEditForm {

    //editForm 취소 위해서.
    @Schema(description = "상품 ID", example = "1")
    private Long id;

    @NotBlank(message = "아이템 이름을 입력하세요.")
    @Schema(description = "상품 이름", example = "아이템A")
    private String itemName;

    //int는 기본형이라 Null 불가, 무조건 값이 들어가고, 사용자가 입력 안하면 0 이 무조건 들어가서 검증이 안됨.
    @NotNull(message = "가격을 입력해주세요")
    @Range(min = 10, max = 999999999, message = "최소 10원, 최대 9억99,999,999 까지 입력 가능합니다.")
    @Schema(description = "상품 가격", example = "2000")
    private Integer price;
    @NotNull(message = "수량을 입력해주세요")
    @Max(value = 9999, message = "최대 수량은 9999개 입니다.")
    @Schema(description = "상품 재고 수량", example = "13")
    private Integer quantity;

    @Positive(message = "카테고리를 다시 선택해주세요.")
    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "상품 이미지 파일(바이너리)")
    private MultipartFile imageFile;

    @Schema(description = "상품 이미지 파일 경로 URL")
    private String imageUrl;

    @Schema(description = "드롭 상품 여부", example = "true")
    private Boolean dropProduct = false;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "드롭 판매 시작 시간", example = "2026-07-01T20:00:00")
    private LocalDateTime dropStartsAt;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Schema(description = "드롭 판매 종료 시간", example = "2026-07-01T21:00:00")
    private LocalDateTime dropEndsAt;

    @Schema(description = "드롭 상품 1인 구매 제한 수량", example = "1")
    private Integer dropPurchaseLimit;

    @Override
    public String toString() {
        return "ItemEditForm{" +
                "id=" + id +
                ", itemName='" + itemName + '\'' +
                ", price=" + price +
                ", quantity=" + quantity +
                ", categoryId=" + categoryId +
                ", imageUrl='" + imageUrl + '\'' +
                ", dropProduct=" + dropProduct +
                ", dropStartsAt=" + dropStartsAt +
                ", dropEndsAt=" + dropEndsAt +
                ", dropPurchaseLimit=" + dropPurchaseLimit +
                '}';
    }
}
