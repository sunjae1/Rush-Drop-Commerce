package myex.shopping.service;

import jakarta.persistence.EntityManager;
import myex.shopping.domain.Category;
import myex.shopping.domain.Item;
import myex.shopping.dto.itemdto.ItemDto;
import myex.shopping.form.ItemAddForm;
import myex.shopping.form.ItemEditForm;
import myex.shopping.repository.ItemRepository;
import myex.shopping.repository.jpa.JpaCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ImageService imageService;

    @Mock
    private JpaCategoryRepository categoryRepository;

    @Mock
    private EntityManager em;

    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemService = new ItemService(itemRepository, categoryRepository, imageService, em);
        itemService = spy(itemService);
    }

    @Test
    @DisplayName("아이템 전체 조회를 DTO로 변환하여 반환한다")
    void findAllToDto() {
        // given
        Item item1 = new Item("itemA", 10000, 10, "images/itemA.jpg");
        item1.setId(1L);
        Item item2 = new Item("itemB", 20000, 20, "images/itemB.jpg");
        item2.setId(2L);
        given(itemRepository.findAll()).willReturn(Arrays.asList(item1, item2));
        given(imageService.resolveImageUrls(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        List<ItemDto> result = itemService.findAllToDto();

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getItemName()).isEqualTo("itemA");
        assertThat(result.get(1).getItemName()).isEqualTo("itemB");
    }

    @Test
    @DisplayName("아이템을 생성하고 저장한다")
    void createItem() throws IOException {
        // given
        ItemAddForm form = new ItemAddForm();
        form.setItemName("New Item");
        form.setPrice(15000);
        form.setQuantity(30);
        form.setCategoryId(7L);
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "test.jpg", "image/jpeg", "test image".getBytes());
        form.setImageFile(imageFile);

        String mockImageKey = "images/mock-image.jpg";
        given(imageService.storeFile(any(MockMultipartFile.class))).willReturn(mockImageKey);
        Category category = new Category();
        category.setId(7L);
        category.setName("상의");
        given(categoryRepository.findById(7L)).willReturn(Optional.of(category));

        Item savedItem = new Item();
        savedItem.setId(1L);
        given(itemRepository.save(any(Item.class))).willReturn(savedItem);

        // when
        Long itemId = itemService.createItem(form);

        // then
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(itemCaptor.capture());
        Item capturedItem = itemCaptor.getValue();

        assertThat(itemId).isEqualTo(1L);
        assertThat(capturedItem.getItemName()).isEqualTo("New Item");
        assertThat(capturedItem.getImageUrl()).isEqualTo(mockImageKey);
        assertThat(capturedItem.getCategory()).isSameAs(category);
    }

    @Test
    @DisplayName("아이템 정보를 수정한다")
    void update() {
        // given
        Long itemId = 1L;
        Item existingItem = new Item("Old Name", 100, 10);
        Item updateParam = new Item("New Name", 200, 20);
        updateParam.setImageUrl("images/new.jpg");

        given(itemRepository.findById(itemId)).willReturn(Optional.of(existingItem));

        // when
        Item updatedItem = itemService.update(itemId, updateParam);

        // then
        assertThat(updatedItem.getItemName()).isEqualTo("New Name");
        assertThat(updatedItem.getPrice()).isEqualTo(200);
        assertThat(updatedItem.getQuantity()).isEqualTo(20);
        assertThat(updatedItem.getImageUrl()).isEqualTo("images/new.jpg");
    }

    @Test
    @DisplayName("아이템 정보를 UUID와 함께 수정한다")
    void editItemWithUUID() throws IOException {

        // given
        Long itemId = 1L;
        ItemEditForm form = new ItemEditForm();
        form.setItemName("Edited Item");
        form.setPrice(25000);
        form.setQuantity(50);
        form.setCategoryId(8L);
        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "edit.png", "image/png", "edited image".getBytes());
        form.setImageFile(imageFile);

        Item existingItem = new Item("Original Item", 10000, 10);
        existingItem.setId(itemId);
        given(itemRepository.findById(itemId)).willReturn(Optional.of(existingItem));
        Category category = new Category();
        category.setId(8L);
        category.setName("하의");
        given(categoryRepository.findById(8L)).willReturn(Optional.of(category));

        String mockImageKey = "images/mock-edited-image.png";
        given(imageService.storeFile(any(MockMultipartFile.class))).willReturn(mockImageKey);

        // when
        Long editedItemId = itemService.editItemWithUUID(form, itemId);

        // then
        ArgumentCaptor<Item> itemCaptor = ArgumentCaptor.forClass(Item.class);
        verify(itemRepository).save(itemCaptor.capture());
        Item capturedItem = itemCaptor.getValue();

        assertThat(editedItemId).isEqualTo(itemId);
        assertThat(capturedItem.getItemName()).isEqualTo("Edited Item");
        assertThat(capturedItem.getPrice()).isEqualTo(25000);
        assertThat(capturedItem.getQuantity()).isEqualTo(50);
        assertThat(capturedItem.getImageUrl()).isEqualTo(mockImageKey);
        assertThat(capturedItem.getCategory()).isSameAs(category);
    }
}
