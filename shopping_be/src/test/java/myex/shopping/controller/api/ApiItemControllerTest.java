package myex.shopping.controller.api;

import jakarta.persistence.EntityManager;
import myex.shopping.domain.Category;
import myex.shopping.domain.Item;
import myex.shopping.domain.ItemDetailImage;
import myex.shopping.domain.ItemDetailImageRole;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.dto.itemdto.ItemDtoDetail;
import myex.shopping.dto.itemdto.ItemEditDto;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import myex.shopping.service.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@WithMockUser
class ApiItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private JpaCategoryRepository categoryRepository;

    @Autowired
    private EntityManager em;

    @MockitoBean
    private ImageService imageService;

    private Item testItem1;
    private Item testItem2;
    private Category topCategory;
    private Category bottomCategory;

    @BeforeEach
    void setUp() throws IOException {
        // ImageService.storeFile이 항상 가짜 S3 key를 반환하도록 설정
        when(imageService.storeFile(any())).thenReturn("images/fake-image.jpg");

        // resolveImageUrls/resolveImageUrl이 입력을 그대로 반환하도록 설정
        when(imageService.resolveImageUrls(any(List.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(ItemDto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(ItemDtoDetail.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.resolveImageUrl(any(ItemEditDto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(imageService.generatePresignedUrl(any())).thenReturn("https://fake-presigned-url.com/image.jpg");

        topCategory = new Category();
        topCategory.setName("상의");
        categoryRepository.save(topCategory);

        bottomCategory = new Category();
        bottomCategory.setName("하의");
        categoryRepository.save(bottomCategory);

        testItem1 = new Item("테스트 상품 1", 10000, 10, "images/test1.jpg");
        testItem1.changeCategory(topCategory);
        testItem2 = new Item("테스트 상품 2", 25000, 5, "images/test2.jpg");
        testItem2.changeCategory(bottomCategory);
        itemRepository.save(testItem1);
        itemRepository.save(testItem2);
    }

    @Test
    @DisplayName("전체 상품 조회 API 테스트: GET /api/items")
    void getItems_shouldReturnItemList() throws Exception {
        mockMvc.perform(get("/api/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].itemName", is(testItem1.getItemName())))
                .andExpect(jsonPath("$[0].categoryName", is(testItem1.getCategory().getName())))
                .andExpect(jsonPath("$[0].dropProduct", is(false)))
                .andExpect(jsonPath("$[0].dropSaleStatus", is("STANDARD")))
                .andExpect(jsonPath("$[0].price", is(testItem1.getPrice())))
                .andExpect(jsonPath("$[1].itemName", is(testItem2.getItemName())));
    }

    @Test
    @DisplayName("카테고리 필터로 상품 목록을 조회할 수 있다")
    void getItems_shouldFilterByCategory() throws Exception {
        mockMvc.perform(get("/api/items")
                        .param("categoryId", topCategory.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].itemName", is(testItem1.getItemName())))
                .andExpect(jsonPath("$[0].categoryId", is(topCategory.getId().intValue())));
    }

    @Test
    @DisplayName("검색어와 카테고리 필터를 함께 적용할 수 있다")
    void getItems_shouldFilterByKeywordAndCategory() throws Exception {
        mockMvc.perform(get("/api/items")
                        .param("categoryId", bottomCategory.getId().toString())
                        .param("keyword", "상품 2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].itemName", is(testItem2.getItemName())))
                .andExpect(jsonPath("$[0].categoryName", is(bottomCategory.getName())));
    }

    @Test
    @DisplayName("삭제된 상품은 기본 상품 목록에서 제외된다")
    void getItems_shouldExcludeDeletedItems() throws Exception {
        itemRepository.deleteItem(testItem1.getId());

        mockMvc.perform(get("/api/items")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(testItem2.getId().intValue())));
    }

    @Test
    @DisplayName("삭제된 상품 조회는 관리자 권한이 필요하다")
    void getItems_shouldReturnForbidden_whenDeletedFilterRequestedByNonAdmin() throws Exception {
        itemRepository.deleteItem(testItem1.getId());

        mockMvc.perform(get("/api/items")
                        .param("deleted", "true")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("개별 상품 조회 API 테스트: GET /api/items/{itemId}")
    void getItem_shouldReturnItem() throws Exception {
        Long itemId = testItem1.getId();

        mockMvc.perform(get("/api/items/{itemId}", itemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.itemName", is(testItem1.getItemName())))
                .andExpect(jsonPath("$.categoryName", is(testItem1.getCategory().getName())))
                .andExpect(jsonPath("$.dropProduct", is(false)))
                .andExpect(jsonPath("$.dropSaleStatus", is("STANDARD")))
                .andExpect(jsonPath("$.price", is(testItem1.getPrice())))
                .andExpect(jsonPath("$.quantity", is(testItem1.getQuantity())));
    }

    @Test
    @DisplayName("개별 상품 조회는 상품별 상세 이미지를 노출 순서대로 반환한다")
    void getItem_shouldReturnDetailImages() throws Exception {
        ItemDetailImage moodImage = new ItemDetailImage(
                1,
                ItemDetailImageRole.MOOD,
                "https://example.com/mood.jpg",
                "테스트 상품 1 착용 컷",
                "착용 컷"
        );
        ItemDetailImage detailImage = new ItemDetailImage(
                2,
                ItemDetailImageRole.DETAIL,
                "https://example.com/detail.jpg",
                "테스트 상품 1 소재 컷",
                "소재 컷"
        );
        testItem1.addDetailImage(detailImage);
        testItem1.addDetailImage(moodImage);
        em.persist(detailImage);
        em.persist(moodImage);
        em.flush();
        em.clear();

        mockMvc.perform(get("/api/items/{itemId}", testItem1.getId())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.detailImages", hasSize(2)))
                .andExpect(jsonPath("$.detailImages[0].displayOrder", is(1)))
                .andExpect(jsonPath("$.detailImages[0].imageRole", is("MOOD")))
                .andExpect(jsonPath("$.detailImages[0].imageUrl", is("https://example.com/mood.jpg")))
                .andExpect(jsonPath("$.detailImages[0].altText", is("테스트 상품 1 착용 컷")))
                .andExpect(jsonPath("$.detailImages[0].caption", is("착용 컷")))
                .andExpect(jsonPath("$.detailImages[1].displayOrder", is(2)))
                .andExpect(jsonPath("$.detailImages[1].imageRole", is("DETAIL")));
    }

    @Test
    @DisplayName("존재하지 않는 상품 조회 시 404 Not Found 반환")
    void getItem_shouldReturnNotFound_whenItemDoesNotExist() throws Exception {
        Long nonExistentItemId = 99999L;

        mockMvc.perform(get("/api/items/{itemId}", nonExistentItemId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 추가 등록 API 테스트: POST /api/items")
    void addItem_shouldCreateItem() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "test-image.jpg", "image/jpeg",
                "image_content".getBytes());

        mockMvc.perform(multipart("/api/items")
                        .file(imageFile)
                        .param("itemName", "새로운 상품")
                        .param("price", "30000")
                        .param("quantity", "20")
                        .param("categoryId", topCategory.getId().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemName", is("새로운 상품")))
                .andExpect(jsonPath("$.categoryId", is(topCategory.getId().intValue())))
                .andExpect(jsonPath("$.categoryName", is(topCategory.getName())))
                .andExpect(jsonPath("$.price", is(30000)))
                .andExpect(jsonPath("$.dropProduct", is(false)))
                .andExpect(jsonPath("$.dropSaleStatus", is("STANDARD")))
                .andExpect(header().exists("Location"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("드롭 상품을 등록하면 드롭 메타데이터와 현재 판매 상태를 반환한다")
    void addItem_shouldCreateDropItem() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "drop-image.jpg", "image/jpeg",
                "image_content".getBytes());
        LocalDateTime startsAt = LocalDateTime.now().minusMinutes(10).withNano(0);
        LocalDateTime endsAt = LocalDateTime.now().plusMinutes(50).withNano(0);

        mockMvc.perform(multipart("/api/items")
                        .file(imageFile)
                        .param("itemName", "한정판 드롭 후디")
                        .param("price", "89000")
                        .param("quantity", "50")
                        .param("categoryId", topCategory.getId().toString())
                        .param("dropProduct", "true")
                        .param("dropStartsAt", startsAt.toString())
                        .param("dropEndsAt", endsAt.toString())
                        .param("dropPurchaseLimit", "1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.itemName", is("한정판 드롭 후디")))
                .andExpect(jsonPath("$.dropProduct", is(true)))
                .andExpect(jsonPath("$.dropPurchaseLimit", is(1)))
                .andExpect(jsonPath("$.dropSaleStatus", is("LIVE")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("드롭 종료 시간이 시작 시간보다 빠르면 400을 반환한다")
    void addItem_shouldRejectInvalidDropWindow() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "drop-image.jpg", "image/jpeg",
                "image_content".getBytes());
        LocalDateTime startsAt = LocalDateTime.now().plusHours(2).withNano(0);
        LocalDateTime endsAt = LocalDateTime.now().plusHours(1).withNano(0);

        mockMvc.perform(multipart("/api/items")
                        .file(imageFile)
                        .param("itemName", "잘못된 드롭 상품")
                        .param("price", "89000")
                        .param("quantity", "50")
                        .param("categoryId", topCategory.getId().toString())
                        .param("dropProduct", "true")
                        .param("dropStartsAt", startsAt.toString())
                        .param("dropEndsAt", endsAt.toString())
                        .param("dropPurchaseLimit", "1")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.dropEndsAt", is("드롭 판매 종료 시간은 시작 시간 이후여야 합니다.")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 수정 API 테스트: PUT /api/items/{id}")
    void editItem_shouldUpdateItem() throws Exception {
        Long itemId = testItem1.getId();
        MockMultipartFile newImageFile = new MockMultipartFile("imageFile", "new-image.jpg", "image/jpeg",
                "new_image_content".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/items/{itemId}", itemId)
                        .file(newImageFile)
                        .param("itemName", "수정된 상품 이름")
                        .param("price", "12000")
                        .param("quantity", "5")
                        .param("categoryId", bottomCategory.getId().toString())
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName", is("수정된 상품 이름")))
                .andExpect(jsonPath("$.categoryId", is(bottomCategory.getId().intValue())))
                .andExpect(jsonPath("$.categoryName", is(bottomCategory.getName())))
                .andExpect(jsonPath("$.price", is(12000)))
                .andExpect(jsonPath("$.dropProduct", is(false)))
                .andExpect(jsonPath("$.dropSaleStatus", is("STANDARD")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("드롭 상품을 일반 상품으로 수정하면 드롭 메타데이터를 제거한다")
    void editItem_shouldClearDropMetadata_whenDropProductIsFalse() throws Exception {
        testItem1.configureDropSale(
                true,
                LocalDateTime.now().minusHours(1).withNano(0),
                LocalDateTime.now().plusHours(1).withNano(0),
                1
        );
        Long itemId = testItem1.getId();
        MockMultipartFile newImageFile = new MockMultipartFile("imageFile", "new-image.jpg", "image/jpeg",
                "new_image_content".getBytes());

        mockMvc.perform(multipart(HttpMethod.PUT, "/api/items/{itemId}", itemId)
                        .file(newImageFile)
                        .param("itemName", "일반 상품으로 전환")
                        .param("price", "12000")
                        .param("quantity", "5")
                        .param("categoryId", bottomCategory.getId().toString())
                        .param("dropProduct", "false")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itemName", is("일반 상품으로 전환")))
                .andExpect(jsonPath("$.dropProduct", is(false)))
                .andExpect(jsonPath("$.dropStartsAt").doesNotExist())
                .andExpect(jsonPath("$.dropEndsAt").doesNotExist())
                .andExpect(jsonPath("$.dropSaleStatus", is("STANDARD")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 삭제 API 테스트: DELETE /api/items/{id}")
    void deleteItem_shouldRemoveItem() throws Exception {
        Long itemId = testItem1.getId();

        mockMvc.perform(delete("/api/items/{itemId}", itemId))
                .andDo(print())
                .andExpect(status().isNoContent());

        assertThat(itemRepository.findById(itemId)).isEmpty();
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("삭제된 상품만 필터로 조회할 수 있다")
    void getItems_shouldReturnOnlyDeletedItems_whenDeletedFilterIsTrue() throws Exception {
        itemRepository.deleteItem(testItem1.getId());

        mockMvc.perform(get("/api/items")
                        .param("deleted", "true")
                        .param("keyword", "상품 1")
                        .param("categoryId", topCategory.getId().toString())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id", is(testItem1.getId().intValue())))
                .andExpect(jsonPath("$[0].itemName", is(testItem1.getItemName())));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("상품 이미지 없이 등록하면 400을 반환한다")
    void addItem_shouldReturnBadRequest_whenImageFileIsMissing() throws Exception {
        mockMvc.perform(multipart("/api/items")
                        .param("itemName", "이미지 없는 상품")
                        .param("price", "30000")
                        .param("quantity", "20")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.imageFile", is("상품 이미지를 선택해주세요.")));
    }
}
