package myex.shopping.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import myex.shopping.dto.cartitemdto.CartItemDto;
import myex.shopping.dto.itemdto.ItemDetailImageDto;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.dto.itemdto.ItemDtoDetail;
import myex.shopping.dto.itemdto.ItemEditDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.presigned-url-expiration:60}")
    private long presignedUrlExpirationMinutes;

    /**
     * 파일을 S3에 업로드하고, S3 key(상대경로)를 반환한다.
     * DB에는 이 key만 저장된다. (예: "images/uuid.png")
     */
    public String storeFile(MultipartFile multipartFile) throws IOException {
        if (multipartFile.isEmpty()) {
            return null;
        }

        String originalFilename = multipartFile.getOriginalFilename();
        String storeFileName = createStoreFileName(originalFilename);
        String key = "images/" + storeFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(multipartFile.getContentType())
                .build();

        s3Client.putObject(putObjectRequest,
                RequestBody.fromInputStream(multipartFile.getInputStream(), multipartFile.getSize()));

        log.info("S3 업로드 완료 - key: {}", key);
        return key;
    }

    /**
     * S3 key(상대경로)로부터 Pre-signed URL을 생성하여 반환한다.
     * 일정 시간(기본 60분) 동안만 유효한 임시 URL이다.
     */
    public String generatePresignedUrl(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(presignedUrlExpirationMinutes))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String url = presignedRequest.url().toString();
        log.debug("Pre-signed URL 생성 - key: {}, expiration: {}분", key, presignedUrlExpirationMinutes);
        return url;
    }

    /**
     * S3 key(상대경로)로 S3 오브젝트를 삭제한다.
     */
    public void deleteFile(String key) {
        if (key == null || key.isBlank() || isDisplayUrl(key)) {
            return;
        }
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("S3 이미지 삭제 완료 - key: {}", key);
        } catch (Exception e) {
            log.warn("S3 이미지 삭제 실패 - key: {}", key, e);
        }
    }

    private String createStoreFileName(String originalFilename) {
        String ext = extractExt(originalFilename);
        String uuid = UUID.randomUUID().toString();
        return uuid + "." + ext;
    }

    private String extractExt(String originalFilename) {
        int pos = originalFilename.lastIndexOf(".");
        return originalFilename.substring(pos + 1);
    }

    // === DTO imageUrl resolve 유틸 메서드 ===

    /**
     * ItemDto 리스트의 imageUrl(S3 key)을 Pre-signed URL로 변환한다.
     */
    public List<ItemDto> resolveImageUrls(List<ItemDto> dtos) {
        dtos.forEach(dto -> dto.setImageUrl(resolveDisplayImageUrl(dto.getImageUrl())));
        return dtos;
    }

    /**
     * 단일 ItemDto의 imageUrl(S3 key)을 Pre-signed URL로 변환한다.
     */
    public ItemDto resolveImageUrl(ItemDto dto) {
        dto.setImageUrl(resolveDisplayImageUrl(dto.getImageUrl()));
        resolveItemDetailImageUrls(dto.getDetailImages());
        return dto;
    }

    /**
     * ItemDtoDetail의 imageUrl(S3 key)을 Pre-signed URL로 변환한다.
     */
    public ItemDtoDetail resolveImageUrl(ItemDtoDetail dto) {
        dto.setImageUrl(resolveDisplayImageUrl(dto.getImageUrl()));
        return dto;
    }

    /**
     * ItemEditDto의 imageUrl(S3 key)을 Pre-signed URL로 변환한다.
     */
    public ItemEditDto resolveImageUrl(ItemEditDto dto) {
        dto.setImageUrl(resolveDisplayImageUrl(dto.getImageUrl()));
        return dto;
    }

    public void resolveCartItemImageUrls(List<CartItemDto> cartItems) {
        if (cartItems == null) {
            return;
        }
        cartItems.stream()
                .map(CartItemDto::getItem)
                .filter(Objects::nonNull)
                .forEach(this::resolveImageUrl);
    }

    private void resolveItemDetailImageUrls(List<ItemDetailImageDto> detailImages) {
        if (detailImages == null) {
            return;
        }
        detailImages.forEach(detailImage ->
                detailImage.setImageUrl(resolveDisplayImageUrl(detailImage.getImageUrl()))
        );
    }

    private String resolveDisplayImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }
        if (isDisplayUrl(imageUrl)) {
            return imageUrl;
        }
        return generatePresignedUrl(imageUrl);
    }

    private boolean isDisplayUrl(String imageUrl) {
        return imageUrl.startsWith("/") ||
                imageUrl.startsWith("http://") ||
                imageUrl.startsWith("https://") ||
                imageUrl.startsWith("data:");
    }
}
