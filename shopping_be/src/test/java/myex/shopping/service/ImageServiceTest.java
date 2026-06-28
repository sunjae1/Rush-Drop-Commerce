package myex.shopping.service;

import myex.shopping.domain.Item;
import myex.shopping.dto.itemdto.ItemDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ImageServiceTest {

    private final S3Client s3Client = mock(S3Client.class);
    private final S3Presigner s3Presigner = mock(S3Presigner.class);
    private final ImageService imageService = new ImageService(s3Client, s3Presigner);

    @Test
    @DisplayName("외부 이미지 URL은 S3 presigned URL 변환 없이 그대로 내려준다")
    void resolveImageUrlKeepsExternalUrl() {
        ItemDto dto = new ItemDto(new Item("드롭 스니커즈", 89000, 8));
        dto.setImageUrl("https://images.pexels.com/photos/1027130/pexels-photo-1027130.jpeg");

        imageService.resolveImageUrl(dto);

        assertThat(dto.getImageUrl())
                .isEqualTo("https://images.pexels.com/photos/1027130/pexels-photo-1027130.jpeg");
        verifyNoInteractions(s3Presigner);
    }

    @Test
    @DisplayName("정적 이미지 경로는 S3 presigned URL 변환 없이 그대로 내려준다")
    void resolveImageUrlKeepsStaticPath() {
        ItemDto dto = new ItemDto(new Item("로컬 이미지 상품", 39000, 4));
        dto.setImageUrl("/image/1.webp");

        imageService.resolveImageUrl(dto);

        assertThat(dto.getImageUrl()).isEqualTo("/image/1.webp");
        verifyNoInteractions(s3Presigner);
    }
}
